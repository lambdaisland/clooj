;; Copyright (c) 2011-2013, Arthur Edelstein
;; All rights reserved.
;; Eclipse Public License 1.0
;; arthuredelstein@gmail.com

(ns clooj.project
  (:require
   [clooj.utils :as utils]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File)
   (java.awt GridLayout)
   (javax.swing JButton JOptionPane JWindow)
   (javax.swing.tree DefaultMutableTreeNode DefaultTreeModel
                     TreePath TreeSelectionModel)
   javax.swing.JTree))

(set! *warn-on-reflection* true)

;; projects tree

(declare restart-doc)

(def project-set (atom (sorted-set)))

(defn docs-tree ^JTree [app]
  (:docs-tree app))

(defn user-object ^File [^DefaultMutableTreeNode node]
  (.getUserObject node))

(defn last-path-component ^DefaultMutableTreeNode [^TreePath tree-path]
  (.getLastPathComponent tree-path))

(defn save-project-set []
  (utils/write-value-to-prefs utils/clooj-prefs "project-set" @project-set))

(defn load-project-set []
  (reset! project-set (into (sorted-set)
                            (utils/read-value-from-prefs utils/clooj-prefs "project-set"))))

(defn tree-path-to-file [^TreePath tree-path]
  (when tree-path
    (try (.getAbsolutePath (user-object (last-path-component tree-path)))
         (catch Exception e nil))))

;; loading and saving expanded paths

(defn get-row-path [^JTree tree row]
  (tree-path-to-file (. tree getPathForRow row)))

(defn get-expanded-paths [^JTree tree]
  (for [i (range (.getRowCount tree))
        :when (.isExpanded tree (long i))]
    (get-row-path tree i)))

(defn save-expanded-paths [^JTree tree]
  (utils/write-value-to-prefs utils/clooj-prefs "expanded-paths" (get-expanded-paths tree)))

(defn expand-paths [^JTree tree paths]
  (doseq [i (range) :while (< i (.getRowCount tree))]
    (when-let [x (some #{(tree-path-to-file (. tree getPathForRow i))} paths)]
      (.expandPath tree (. tree getPathForRow i)))))

(defn load-expanded-paths [^JTree tree]
  (let [paths (utils/read-value-from-prefs utils/clooj-prefs "expanded-paths")]
    (when paths
      (expand-paths tree paths))))

;; loading and saving tree selection

(defn save-tree-selection [^JTree tree path]
  (utils/write-value-to-prefs
   utils/clooj-prefs "tree-selection"
   (tree-path-to-file path)))

(defn path-components
  "Generates a sequence of the components in a file path."
  [the-file]
  (->>
    (-> the-file
        io/file
        .getAbsolutePath
        (.split File/separator))
    (remove empty?)
    (remove #(= % "."))))

(defn file-ancestor?
  "In the file tree, returns true if descendant-file
   is a direct descendant of ancestor-file.
   Also returns true if the files are the same."
  [ancestor-file descendant-file]
  (let [ancestor (path-components ancestor-file)
        descendant (path-components descendant-file)]
    (and (every? true? (map = ancestor descendant))
         (<= (count ancestor) (count descendant)))))

(defn node-children [^DefaultMutableTreeNode node]
  (when-not (.isLeaf node)
    (for [i (range (.getChildCount node))]
      (.getChildAt node i))))

(defn path-to-node
  "Find the tree node corresponding to a particular file path."
  ^DefaultMutableTreeNode [^JTree tree path]
  (let [^DefaultMutableTreeNode root-node (.. tree getModel getRoot)]
    (loop [node root-node]
      (when (and node (not (.isLeaf node)))
        (when-let [children (node-children node)]
          (let [closer-node (first
                             (filter #(file-ancestor?
                                       (user-object ^DefaultMutableTreeNode %) path)
                                     children))]
            (when closer-node
              (if (= (io/file path)
                     (user-object closer-node))
                closer-node
                (recur closer-node)))))))))

(defn row-for-path [^JTree tree path]
  (first
   (for [i (range 1 (.getRowCount tree))
         :when (= path
                  (-> tree (.getPathForRow i)
                      .getPath last user-object .getAbsolutePath))]
     i)))

(defn set-tree-selection [^JTree tree path]
  (utils/awt-event
    (when-let [node (path-to-node tree path)]
      (let [node-path (.getPath node)
            paths (map #(-> % user-object .getAbsolutePath) (rest node-path))]
        (expand-paths tree paths)
        (when-let [row (row-for-path tree path)]
          (.setSelectionRow tree row))))))

(defn load-tree-selection [^JTree tree]
  (let [path (utils/read-value-from-prefs utils/clooj-prefs "tree-selection")]
    (if (nil? path)
      false
      (do
        (set-tree-selection tree path)
        true))))

;;;;;;;;;;;;;;;;;;;

(defn get-code-files [dir suffix]
  (let [dir (io/file dir)]
    (sort (filter #(str/ends-with? (.getName ^File %) suffix)
                  (file-seq dir)))))

(defn get-temp-file ^File [^File orig]
  (when orig
    (io/file (str (.getAbsolutePath orig) "~"))))

(defn get-projects
  "Load projects from preferences, and return
   a sorted vector."
  []
  (->> (utils/read-value-from-prefs utils/clooj-prefs "project-set")
      set
      (sort-by #(.toLowerCase (.getName (io/file %))))
      vec))

(defn visible-children
  "Get a vector of a directory's children, if there are any.
   Omits hidden and temporary files."
  [^File file]
  (->> (.listFiles file)
       (remove #(.startsWith (.getName ^File %) "."))
       (remove #(str/ends-with? (.getName ^File %) "~"))
       vec))

(defn file-name-text
  "Show a file's name, with *stars* if it is the temp file."
  [file]
  (if (.exists (get-temp-file file))
    (str "*" (.getName ^File file) "*")
    (str (.getName ^File file) "    ")))

(defn file-node
  "Tree node representing a file (possibly a directory)."
  [^File file]
  (let [children (delay (visible-children file))]
    (proxy [DefaultMutableTreeNode] [file]
      (getChildAt [i] (file-node (@children i)))
      (getChildCount [] (count @children))
      (toString [] (file-name-text file))
      (isLeaf [] (not (.isDirectory file))))))

(defn root-node
  "The root tree node, given a vector of project locations."
  [projects]
  (proxy [DefaultMutableTreeNode] []
    (getChildAt [i] (file-node (io/file (nth projects i))))
    (getChildCount [] (count projects))
    (toString [] "root")))

(defn file-tree-model [projects]
    (DefaultTreeModel. (root-node projects) false))

(defn update-project-tree [^JTree tree]
  (let [model (file-tree-model (vec @project-set))]
    (utils/awt-event
     (.setModel tree model)
     (save-project-set)
     (load-expanded-paths tree)
     (load-tree-selection tree)
     (save-expanded-paths tree))))

(defn get-selected-file-path ^String [app]
  (when-let [^TreePath tree-path (-> app docs-tree .getSelectionPaths first)]
    (-> tree-path last-path-component user-object .getAbsolutePath)))

(defn get-selected-namespace [^JTree tree]
  (-> tree .getSelectionPaths first
      last-path-component user-object .toString
      (str/replace #"\.clj" "") (str/replace #"/" ".")))

(defn get-selected-projects [app]
  (let [selections (.getSelectionPaths (docs-tree app))]
    (for [^TreePath selection selections]
      (-> selection .getPath second user-object))))

(defn add-project [app project-path]
  (swap! project-set conj project-path))

(defn rename-project [app]
  (when-let [dir (utils/choose-file (app :frame) "Move/rename project directory" "" false)]
    (let [old-project (first (get-selected-projects app))]
      (if (.renameTo (io/file old-project) dir)
        (do
          (swap! project-set
                 #(-> % (disj old-project) (conj (.getAbsolutePath dir))))
          (update-project-tree (docs-tree app)))
        (JOptionPane/showMessageDialog nil "Unable to move project.")))))

(defn remove-selected-project [app]
  (apply swap! project-set disj (map #(.getAbsolutePath ^File %)
                                     (get-selected-projects app)))
  (update-project-tree (docs-tree app)))

