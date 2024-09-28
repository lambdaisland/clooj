;; Copyright (c) 2011-2013, Arthur Edelstein
;; All rights reserved.
;; Eclipse Public License 1.0
;; arthuredelstein@gmail.com

(ns clooj.help
  (:require
   [clojure.repl]
   [clojure.string :as str]
   [clooj.brackets :as brackets]
   [clooj.clj-inspector.jars :as jars]
   [clooj.clj-inspector.vars :as vars]
   [clooj.protocols :as proto]
   [clooj.text-area :as text-area]
   [clooj.utils :as utils])
  (:import
   (java.awt Color Point)
   (java.io File)
   (java.lang.reflect Field Method Modifier)
   (java.util Vector)
   (javax.swing DefaultListCellRenderer JLabel JList JScrollPane JSplitPane ListSelectionModel)
   (javax.swing.event ListSelectionListener)))

(set! *warn-on-reflection* true)

(def var-maps-agent (agent nil))

;; from http://clojure.org/special_forms
(def special-forms
  '{def "(def symbol init?)"
    if  "(if test then else?)"
    do  "(do exprs*)"
    let "(let [bindings* ] exprs*)"
    quote "(quote form)"
    var "(var symbol)"
    fn  "(fn name? [params* ] exprs*)"
    loop "(loop [bindings* ] exprs*)"
    recur "(recur exprs*)"
    throw "(throw expr)"
    try   "(try expr* catch-clause* finally-clause?)"
    catch "(catch classname name expr*)"
    monitor-enter "Avoid!"
    monitor-exit  "Avoid!"})

(defn- present-item ^String [item]
  (str (:name item) " [" (:ns item) "]"))

(defn- make-var-super-map [var-maps]
  (into {}
        (for [var-map var-maps]
          [[(:ns var-map) (:name var-map)] var-map])))

(defn- classpath-to-jars [project-path classpath]
  (apply
   concat
   (for [item classpath]
     (cond (str/ends-with? item "*") (jars/jar-files (apply str (butlast item)))
           (str/ends-with? item ".jar") (list (File. ^String item))
           :else (jars/jar-files item)))))

(defn- get-sources-from-jars [project-path classpath]
  (->> (classpath-to-jars project-path classpath)
       (mapcat jars/clj-sources-from-jar)
       merge
       vals))

(defn- get-sources-from-clj-files [classpath]
  (map slurp
       (apply concat
              (for [item classpath]
                (let [item-file (File. ^String item)]
                  (when (.isDirectory item-file)
                    (filter #(str/ends-with? (.getName ^File %) ".clj")
                            (file-seq item-file))))))))

(defn- get-var-maps [project-path classpath]
  (make-var-super-map
   (mapcat #(vars/analyze-clojure-source "clj" %)
           (concat
            (get-sources-from-jars project-path classpath)
            (get-sources-from-clj-files classpath)))))

(defn- update-var-maps! [project-path classpath]
  (send-off var-maps-agent #(merge % (get-var-maps project-path classpath))))

(defn- find-form-string [text pos]
  (let [[left right] (brackets/find-enclosing-brackets text pos)]
    (when (> (count text) left)
      (subs text (inc left)))))

(def non-token-chars [\; \~ \@ \( \) \[ \] \{ \} \  \. \newline \/ \" \'])

(defn- local-token-location [text ^long pos]
  (let [n (long (count text))
        pos (Math/min (Math/max pos 0) n)]
    [(loop [p (dec pos)]
       (if (or (neg? p)
               (some #{(.charAt ^String text p)} non-token-chars))
         (inc p)
         (recur (dec p))))
     (loop [p pos]
       (if (or (>= p n)
               (some #{(.charAt ^String text p)} non-token-chars))
         p
         (recur (inc p))))]))

(defn- head-token [form-string]
  (when form-string
    (second
     (re-find #"([^;]*?)[\s|\)|$]"
              (str (str/trim form-string) " ")))))

(defn- current-ns-form [app]
  (-> app :doc-text-area text-area/text read-string))

;; tab help

(defonce help-state (atom {:visible false :token nil :pos nil}))

(defn- var-map [v]
  (when-let [m (meta v)]
    (let [ns (:ns m)]
      (-> m
          (select-keys [:doc :ns :name :arglists])
          (assoc :source (binding [*ns* ns]
                           (clojure.repl/source-fn (symbol (str ns "/" name)))))))))

(defn- var-help [var-map]
  (let [{:keys [doc ns name arglists source]} var-map]
    (str name
         (if ns (str " [" ns "]") "") "\n"
         arglists
         "\n\n"
         (if doc
           (str "Documentation:\n" doc)
           "No documentation found.")
         "\n\n"
         (if source
           (str "Source:\n"
                (if doc
                  (.replace ^String source ^String doc "...docs...")
                  source))
           "No source found."))))

(defn- create-param-list
  ([^Method method-or-constructor static]
   (str " (["
        (let [type-names (map #(.getSimpleName ^Class %)
                              (.getParameterTypes method-or-constructor))
              param-names (if static type-names (cons "this" type-names))]
          (apply str (interpose " " param-names)))
        "])"))
  ([method-or-constructor]
   (create-param-list method-or-constructor true)))

(defn- constructor-help [^Method constructor]
  (str (.. constructor getDeclaringClass getSimpleName) "."
       (create-param-list constructor)))

(defn- method-help [^Method method]
  (let [stat (Modifier/isStatic (.getModifiers method))]
    (str
     (if stat
       (str (.. method getDeclaringClass getSimpleName)
            "/" (.getName method))
       (str "." (.getName method)))
     (create-param-list method stat)
     " --> " (.getName (.getReturnType method)))))

(defn- field-help [^Field field]
  (let [c (.. field getDeclaringClass getSimpleName)]
    (str
     (if (Modifier/isStatic (.getModifiers field))
       (str (.. field getDeclaringClass getSimpleName)
            "/" (.getName field)
            (when (Modifier/isFinal (.getModifiers field))
              (str " --> " (.. field (get nil) toString))))
       (str "." (.getName field) " --> " (.getName (.getType field)))))))

(defn- class-help [^Class c]
  (apply str
         (concat
          [(present-item c) "\n  java class"]
          ["\n\nCONSTRUCTORS\n"]
          (interpose "\n"
                     (sort
                      (for [constructor (.getConstructors c)]
                        (constructor-help constructor))))
          ["\n\nMETHODS\n"]
          (interpose "\n"
                     (sort
                      (for [method (.getMethods c)]
                        (method-help method))))
          ["\n\nFIELDS\n"]
          (interpose "\n"
                     (sort
                      (for [field (.getFields c)]
                        (field-help field)))))))

(defn- item-help [item]
  (cond (map? item) (var-help item)
        (class? item) (class-help item)))

(defn- set-first-component [^JSplitPane split-pane comp]
  (let [loc (.getDividerLocation split-pane)]
    (.setTopComponent split-pane comp)
    (.setDividerLocation split-pane loc)))

(defn- clock-num [i n]
  (if (zero? n)
    0
    (cond (< i 0) (dec n)
          (>= i n) 0
          :else i)))

(defn- list-size [^JList list]
  (-> list .getModel .getSize))

(defn- match-items [pattern items]
  (->> items
       (filter #(re-find pattern (:name %)))
       (sort-by #(str/lower-case (:name %)))))

(defn- hits [token]
  (let [token-pat1 (re-pattern (str "(?i)\\A\\Q" token "\\E"))
        token-pat2 (re-pattern (str "(?i)\\A.\\Q" token "\\E"))
        items (vals @var-maps-agent)
        best (match-items token-pat1 items)
        others (match-items token-pat2 items)]
    (concat best others)))

(defn- show-completion-list [{:keys [^JList completion-list
                                    repl-split-pane
                                    help-text-scroll-pane
                                    doc-split-pane
                                    completion-panel
                                    ^JLabel repl-label]
                             :as app}]
  (when (pos? (list-size completion-list))
    (set-first-component repl-split-pane help-text-scroll-pane)
    (set-first-component doc-split-pane completion-panel)
    (.setText repl-label "Documentation")
    (.ensureIndexIsVisible completion-list
                           (.getSelectedIndex completion-list))))

(defn- advance-help-list [app token index-change-fn]
  (let [^JList help-list (app :completion-list)]
    (if (not= token (@help-state :token))
      (do
        (swap! help-state assoc :token token)
        (.setListData help-list (Vector. ^java.util.List (hits token)))
        (.setSelectedIndex help-list 0))
      (let [n (list-size help-list)]
        (when (pos? n)
          (.setSelectedIndex help-list
                             (clock-num
                              (index-change-fn
                               (.getSelectedIndex help-list))
                              n))))))
  (show-completion-list app))

(defn- get-list-item [app]
  (.getSelectedValue ^JList (:completion-list app)))

(defn- get-list-artifact [app]
  (when-let [artifact (:artifact (get-list-item app))]
    (binding [*read-eval* false]
      (read-string artifact))))

(defn- get-list-token [app]
  (let [val (get-list-item app)]
    (str (:ns val) "/" (:name val))))

(defn- show-help-text [app choice]
  (let [help-text (or (when choice (item-help choice)) "")]
    (text-area/set-text (app :help-text-area) help-text))
  (.setViewPosition
   (.getViewport ^JScrollPane (:help-text-scroll-pane app))
   (Point. (int 0) (int 0))))

(defn- hide-tab-help [app]
  (utils/awt-event
    (when (@help-state :visible)
      (set-first-component (app :repl-split-pane)
                           (app :repl-out-scroll-pane))
      (set-first-component (app :doc-split-pane)
                           (app :docs-tree-panel))
      (.setText ^JLabel (app :repl-label) "Clojure REPL output"))
    (swap! help-state assoc :visible false :pos nil)))

(defn- update-ns-form [app]
  (current-ns-form app))

(defn- add-classpath-to-repl
  [app files]
  (.addAll ^java.util.concurrent.BlockingQueue (app :classpath-queue)
           files))

(defn- load-dependencies [app artifact]
  (utils/awt-event (utils/append-text (app :repl-out-text-area)
                                      (str "\nLoading " artifact " ... ")))
  ;; FIXME: use lambdaisland.classpath
  ;; (let [deps (cemerick.pomegranate.aether/resolve-dependencies
  ;;              :coordinates [artifact]
  ;;              :repositories
  ;;                (merge aether/maven-central
  ;;                       {"clojars" "http://clojars.org/repo"}))]
  ;;   (add-classpath-to-repl app (aether/dependency-files deps)))
  (utils/append-text (app :repl-out-text-area)
                     (str "done.")))

(defn- update-token [app text-comp new-token]
  (utils/awt-event
    (let [[start stop] (local-token-location
                        (utils/get-text-str text-comp)
                        (text-area/caret-position text-comp))
          len (- stop start)]
      (when (and (seq new-token) (pos? (.getSize (.getModel ^JList (:completion-list app)))))
        (text-area/replace-str text-comp start len new-token)))))

(defn- resolve-var-info [repl {:keys [aliases mappings] :as ns-info} var-sym]
  (doto (if (qualified-symbol? var-sym)
          (or (when-let [alias (get aliases (symbol (namespace var-sym)))]
                (proto/var-info repl (symbol (str alias) (name var-sym))))
              (proto/var-info repl var-sym))
          (or (proto/var-info repl (symbol (str (:name ns-info)) (str var-sym)))
              (proto/var-info repl (get mappings var-sym))))
    tap>))

;; Public API

(defn token-from-caret-pos [text pos]
  (let [t (head-token (find-form-string text pos))]
    (when-not (str/blank? t)
      t)))

(defn arglist-from-caret-pos [app ns text pos]
  (when-let [token (token-from-caret-pos text pos)]
    (let [token (symbol token)]
      (if-let [s (get special-forms token)]
        s
        (let [repl @(:repl app)
              ns-info (proto/ns-info repl (symbol ns))
              {:keys [ns name arglists] :as var-info} (resolve-var-info repl ns-info token)]
          (str/join " " (map #(cons (symbol (str ns) (str name)) %) arglists)))))))

(defn show-tab-help [app text-comp index-change-fn]
  (def show-tab-help* [app text-comp index-change-fn])
  (utils/awt-event
    (let [text (utils/get-text-str text-comp)
          pos (text-area/caret-position text-comp)
          [start stop] (local-token-location text pos)]
      (when-let [token (subs text start stop)]
        (swap! help-state assoc :pos start :visible true)
        (advance-help-list app token index-change-fn)))))


(defn help-handle-caret-move [app text-comp]
  (def help-handle-caret-move* [app text-comp])
  (utils/awt-event
    (when (@help-state :visible)
      (let [[start _] (local-token-location (utils/get-text-str text-comp)
                                            (text-area/caret-position text-comp))]
        (if-not (= start (@help-state :pos))
          (hide-tab-help app)
          (show-tab-help app text-comp identity))))))

(defn setup-tab-help [text-comp app]
  (def setup-tab-help* [text-comp app])
  (utils/attach-action-keys text-comp
                            ["TAB" #(show-tab-help app text-comp inc)]
                            ["shift TAB" #(show-tab-help app text-comp dec)]
                            ["ESCAPE" #(hide-tab-help app)])
  (utils/attach-child-action-keys text-comp
                                  ["ENTER" #(@help-state :visible)
                                   #(do (hide-tab-help app)
                                        (.start (Thread. (fn [] (load-dependencies app (get-list-artifact app)))))
                                        (update-token app text-comp (get-list-token app)))]))

(defn find-focused-text-pane [app]
  (def find-focused-text-pane* [app])
  (let [t1 (app :doc-text-area)
        t2 (app :repl-in-text-area)]
    (cond (text-area/focus? t1) t1
          (text-area/focus? t2) t2)))

(defn setup-completion-list [^JList l app]
  (def  setup-completion-list* [^JList l app])
  (doto l
    (.setBackground (Color. 0xFF 0xFF 0xE8))
    (.setFocusable false)
    (.setSelectionMode ListSelectionModel/SINGLE_SELECTION)
    (.setCellRenderer
     (proxy [DefaultListCellRenderer] []
       (getListCellRendererComponent [^JList list item index isSelected cellHasFocus]
         (let [^DefaultListCellRenderer this this
               ^DefaultListCellRenderer renderer (proxy-super getListCellRendererComponent list item index isSelected cellHasFocus)]
           (.setText renderer (present-item item))))))
    (.addListSelectionListener
     (reify ListSelectionListener
       (valueChanged [_ e]
         (when-not (.getValueIsAdjusting e)
           (.ensureIndexIsVisible l (.getSelectedIndex l))
           (show-help-text app (.getSelectedValue l))))))
    (utils/on-click 2 #(when-let [text-pane (find-focused-text-pane app)]
                         (update-token app text-pane (get-list-token app))))))
