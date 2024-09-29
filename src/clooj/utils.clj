;; Copyright (c) 2011-2013, Arthur Edelstein
;; All rights reserved.
;; Eclipse Public License 1.0
;; arthuredelstein@gmail.com

(ns clooj.utils
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.awt FileDialog Point Window)
   (java.awt.event ActionListener MouseAdapter)
   (java.io BufferedReader ByteArrayInputStream ByteArrayOutputStream File FilenameFilter InputStreamReader ObjectInputStream ObjectOutputStream OutputStream PrintStream Writer)
   (java.lang Process)
   (java.security MessageDigest)
   (java.util Timer TimerTask)
   (java.util.prefs Preferences)
   (javax.swing AbstractAction BorderFactory JButton JComponent JFileChooser JFrame JMenu JMenuBar JMenuItem JOptionPane JSplitPane JViewport KeyStroke SpringLayout SwingUtilities)
   (javax.swing.event CaretListener DocumentListener UndoableEditListener)
   (javax.swing.undo UndoManager)
   (org.fife.ui.rsyntaxtextarea RSyntaxDocument RSyntaxTextArea)))

(set! *warn-on-reflection* true)

;; general

(defmacro do-when [f & args]
  (let [args_ args]
    `(when (and ~@args_)
       (~f ~@args_))))

(defmacro when-lets [bindings & body]
  (assert (vector? bindings))
  (let [n (count bindings)]
    (assert (zero? (mod n 2)))
    (assert (<= 2 n))
    (if (= 2 n)
      `(when-let ~bindings ~@body)
      (let [[a b] (map vec (split-at 2 bindings))]
        `(when-let ~a (when-lets ~b ~@body))))))

(defn count-while [pred coll]
  (count (take-while pred coll)))

(defn remove-nth [s n]
  (lazy-cat (take n s) (drop (inc n) s)))

(defmacro awt-event [& body]
  `(SwingUtilities/invokeLater
    (fn [] (try ~@body
                (catch Throwable t#
                  (.printStackTrace t#))))))

(defmacro gen-map [& args]
  (let [kw (map keyword args)]
    (zipmap kw args)))

(defn class-for-name
  "Returns true if a class represented by class-name
   can be found by the class loader."
  [class-name]
  (try (Class/forName class-name)
       (catch Throwable _ nil)))

;; preferences

;; define a UUID for clooj preferences
(def clooj-prefs (.. Preferences userRoot
                     (node "clooj") (node "c6833c87-9631-44af-af83-f417028ea7aa")))

(defn partition-str [n ^String s]
  (let [l (count s)]
    (for [i (range 0 l n)]
      (subs s i (Math/min (long l) (+ (int i) (int n)))))))

(def pref-max-bytes (* 3/4 Preferences/MAX_VALUE_LENGTH))

(defn write-value-to-prefs
  "Writes a pure clojure data structure to Preferences object."
  [^Preferences prefs key value]
  (let [chunks (partition-str pref-max-bytes (with-out-str (pr value)))
        node (. prefs node key)]
    (.clear node)
    (doseq [i (range (count chunks))]
      (. node put (str i) (nth chunks i)))))

(defn read-value-from-prefs
  "Reads a pure clojure data structure from Preferences object."
  [^Preferences prefs ^String key]
  (when-not (str/ends-with? key "/")
    (let [node (.node prefs key)
          ^String s (apply str
                           (for [i (range (count (. node keys)))]
                             (.get node (str i) nil)))]
      (when (and s (pos? (count s))) (read-string s)))))

(defn write-obj-to-prefs
  "Writes a java object to a Preferences object."
  [^Preferences prefs key obj]
  (let [bos (ByteArrayOutputStream.)
        os (ObjectOutputStream. bos)
        node (.node prefs key)]
    (.writeObject os obj)
    (. node putByteArray "0" (.toByteArray bos))))

(defn read-obj-from-prefs
  "Reads a java object from a Preferences object."
  [^Preferences prefs key]
  (let [node (.node prefs key)
        bis (ByteArrayInputStream. (. node getByteArray "0" nil))
        os (ObjectInputStream. bis)]
    (.readObject os)))

;; identify OS

(defn get-os ^String []
  (str/lower-case (System/getProperty "os.name")))

(def is-win
  (memoize #(not (neg? (.indexOf (get-os) "win")))))

(def is-mac
  (memoize #(not (neg? (.indexOf (get-os) "mac")))))

(def is-unix
  (memoize #(not (and (neg? (.indexOf (get-os) "nix"))
                      (neg? (.indexOf (get-os) "nux"))))))

;; swing layout

(defn put-constraint [^java.awt.Component comp1 edge1 ^java.awt.Component comp2 edge2 dist]
  (let [edges {:n SpringLayout/NORTH
               :w SpringLayout/WEST
               :s SpringLayout/SOUTH
               :e SpringLayout/EAST}
        ^SpringLayout layout (.. comp1 getParent getLayout
                                 )]
    (.putConstraint layout ^String (edges edge1) comp1 (int dist) ^String (edges edge2) comp2)))

(defn put-constraints [comp & args]
  (let [args (partition 3 args)
        edges [:n :w :s :e]]
    (dorun (map #(apply put-constraint comp %1 %2) edges args))))

(defn constrain-to-parent
  "Distance from edges of parent comp args"
  [comp & args]
  (apply put-constraints comp
         (flatten (map #(cons (.getParent ^JComponent comp) %) (partition 2 args)))))

;; other gui

(defn make-split-pane ^JSplitPane [comp1 comp2 horizontal divider-size resize-weight]
  (doto (JSplitPane. (if horizontal JSplitPane/HORIZONTAL_SPLIT
                         JSplitPane/VERTICAL_SPLIT)
                     true comp1 comp2)
    (.setResizeWeight resize-weight)
    (.setOneTouchExpandable false)
    (.setBorder (BorderFactory/createEmptyBorder))
    (.setDividerSize divider-size)))

;; keys

(defn get-keystroke ^KeyStroke [^String key-shortcut]
  (KeyStroke/getKeyStroke
   (-> key-shortcut
       (.replace "cmd1" (if (is-mac) "meta" "ctrl"))
       (.replace "cmd2" (if (is-mac) "ctrl" "alt")))))

;; actions

(defn attach-child-action-key
  "Maps an input-key on a swing component to an action,
  such that action-fn is executed when pred function is
  true, but the parent (default) action when pred returns
  false."
  [^JComponent component input-key pred action-fn]
  (let [im (.getInputMap component)
        am (.getActionMap component)
        input-event (get-keystroke input-key)
        parent-action (when-let [tag (.get im input-event)]
                        (.get am tag))
        child-action
        (proxy [AbstractAction] []
          (actionPerformed [e]
            (if (pred)
              (action-fn)
              (when parent-action
                (.actionPerformed parent-action e)))))
        uuid (str (random-uuid))]
    (.put im input-event uuid)
    (.put am uuid child-action)))

(defn attach-child-action-keys [^JComponent comp & items]
  (run! #(apply attach-child-action-key comp %) items))

(defn attach-action-key
  "Maps an input-key on a swing component to an action-fn."
  [^JComponent component input-key action-fn]
  (attach-child-action-key component input-key
                           (constantly true) action-fn))

(defn attach-action-keys
  "Maps input keys to action-fns."
  [^JComponent comp & items]
  (run! #(apply attach-action-key comp %) items))

;; buttons

(defn create-button ^JButton [^String text fn]
  (doto (JButton. text)
    (.addActionListener
     (reify ActionListener
       (actionPerformed [_ _] (fn))))))

;; menus

(defn add-menu-item
  ([^JMenu menu ^String item-name key-mnemonic key-accelerator response-fn]
   (let [menu-item (JMenuItem. item-name)]
     (when key-accelerator
       (.setAccelerator menu-item (get-keystroke key-accelerator)))
     (when (and (not (is-mac)) key-mnemonic)
       (.setMnemonic menu-item (.getKeyCode (get-keystroke key-mnemonic))))
     (.addActionListener menu-item
                         (reify ActionListener
                           (actionPerformed [this action-event]
                             (response-fn))))
     (.add menu menu-item)))
  ([^JMenu menu item]
   (condp = item
     :sep (.addSeparator menu))))

(defn add-menu
  "Each item-tuple is a vector containing a
  menu item's text, mnemonic key, accelerator key, and the function
  it executes."
  [^JMenuBar menu-bar ^String title key-mnemonic & item-tuples]
  (let [menu (JMenu. title)]
    (when (and (not (is-mac)) key-mnemonic)
      (.setMnemonic menu (.getKeyCode (get-keystroke key-mnemonic))))
    (run! #(apply add-menu-item menu %) item-tuples)
    (.add menu-bar menu)
    menu))

;; mouse

(defn on-click [^JComponent comp num-clicks fun]
  (.addMouseListener comp
                     (proxy [MouseAdapter] []
                       (mouseClicked [^java.awt.event.MouseEvent event]
                         (when (== num-clicks (.getClickCount event))
                           (.consume event)
                           (fun))))))

;; undoability

(defn make-undoable [^RSyntaxTextArea text-area]
  (let [undoMgr (UndoManager.)]
    (.setLimit undoMgr 1000)
    (.. text-area getDocument (addUndoableEditListener
                               (reify UndoableEditListener
                                 (undoableEditHappened [this evt] (.addEdit undoMgr (.getEdit evt))))))
    (attach-action-keys text-area
                        ["cmd1 Z" #(when (.canUndo undoMgr) (.undo undoMgr))]
                        ["cmd1 shift Z" #(when (.canRedo undoMgr) (.redo undoMgr))])))


;; file handling

(defn choose-file ^File [^JFrame parent ^String title suffix load]
  (let [dialog
        (doto (FileDialog. parent title
                           (if load FileDialog/LOAD FileDialog/SAVE))
          (.setFilenameFilter
           (reify FilenameFilter
             (accept [this _ name] (. name endsWith suffix))))
          (.setVisible true))
        d (.getDirectory dialog)
        n (.getFile dialog)]
    (when (and d n)
      (File. d n))))

(defn choose-directory [parent title]
  (let [fc (JFileChooser.)
        last-open-dir (read-value-from-prefs clooj-prefs "last-open-dir")]
    (doto fc (.setFileSelectionMode JFileChooser/DIRECTORIES_ONLY)
          (.setDialogTitle title)
          (.setCurrentDirectory (if last-open-dir (File. ^String last-open-dir) nil)))
    (when (= JFileChooser/APPROVE_OPTION (.showOpenDialog fc parent))
      (.getSelectedFile fc))))


(defn get-directories [^File path]
  (filter #(and (.isDirectory ^File %)
                (not (.startsWith (.getName ^File %) ".")))
          (.listFiles path)))

(defn file-exists? [^File file]
  (and file (.. file exists)))

;; tree seq on widgets (awt or swing)

(defn widget-seq [^java.awt.Component comp]
  (tree-seq #(instance? java.awt.Container %)
            #(seq (.getComponents ^java.awt.Container %))
            comp))

;; saving and restoring window shape in preferences

(defn get-shape [components]
  (for [comp components]
    (condp instance? comp
      Window
      (let [comp ^Window comp]
        [:window {:x (.getX comp) :y (.getY comp)
                  :w (.getWidth comp) :h (.getHeight comp)}])
      JSplitPane
      [:split-pane {:location (.getDividerLocation ^JSplitPane comp)}]
      nil)))

(defn watch-shape [components fun]
  (doseq [comp components]
    (condp instance? comp
      Window
      (.addComponentListener ^Window comp
                             (proxy [java.awt.event.ComponentAdapter] []
                               (componentMoved [_] (fun))
                               (componentResized [_] (fun))))
      JSplitPane
      (.addPropertyChangeListener ^JSplitPane comp JSplitPane/DIVIDER_LOCATION_PROPERTY
                                  (proxy [java.beans.PropertyChangeListener] []
                                    (propertyChange [_] (fun))))
      nil)))

(defn set-shape [components shape-data]
  (loop [comps components shapes shape-data]
    (let [comp (first comps)
          shape (first shapes)]
      (try
        (when shape
          (condp = (first shape)
            :window
            (let [{:keys [x y w h]} (second shape)]
              (.setBounds ^Window comp x y w h))
            :split-pane
            (.setDividerLocation ^JSplitPane comp (int (:location (second shape))))
            nil))
        (catch Exception e nil)))
    (when (next comps)
      (recur (next comps) (next shapes)))))

(defn save-shape [prefs name components]
  (write-value-to-prefs prefs name (get-shape components)))

(defn restore-shape [prefs name components]
  (try
    (set-shape components (read-value-from-prefs prefs name))
    (catch Exception e)))

(defn confirmed? [question title]
  (= JOptionPane/YES_OPTION
     (JOptionPane/showConfirmDialog
      nil question title  JOptionPane/YES_NO_OPTION)))

(defn ask-value [question title]
  (JOptionPane/showInputDialog nil question title JOptionPane/QUESTION_MESSAGE))

(defn persist-window-shape [prefs name ^java.awt.Window window]
  (let [components (widget-seq window)
        shape-persister (agent nil)]
    (restore-shape prefs name components)
    (watch-shape components
                 #(send-off shape-persister
                            (fn [old-shape]
                              (let [shape (get-shape components)]
                                (when (not= old-shape shape)
                                  (write-value-to-prefs prefs name shape))
                                shape))))))

(defn sha1-str [obj]
  (let [^String s (with-out-str (pr obj))
        bytes (.getBytes s)]
    (String. (.digest (MessageDigest/getInstance "MD") bytes))))

;; streams, writers and readers

(defn printstream-to-writer [^Writer writer]
  (->
   (proxy [OutputStream] []
     (write
       ([^bytes bs offset length]
        (.write writer
                (.toCharArray (String. ^bytes bs "utf-8"))
                (long offset) (long length)))
       ([b]
        (.write writer (int b))))
     (flush [] (.flush writer))
     (close [] (.close writer)))
   (PrintStream. true)))

(defn process-reader
  "Create a buffered reader from the output of a process."
  [^Process process]
  (-> process
      .getInputStream
      InputStreamReader.
      BufferedReader.))

(defn copy-input-stream-to-writer
  "Continuously copies all content from a java InputStream
   to a java Writer. Blocks until InputStream closes."
  [input-stream ^Writer writer]
  (let [reader (InputStreamReader. input-stream)]
    (loop []
      (let [c (.read reader)]
        (when (not= c -1)
          (.write writer c)
          (recur))))))

;; .clj file in current jar

(defn local-clj-source
  "Reads a clj source file inside a jar from the current classpath."
  [clj-file]
  (try
    (slurp (io/resource clj-file))
    (catch Exception _ nil)))

(defn debounce [f ms]
  (let [timer (Timer. "debounce")
        !task (volatile! nil)]
    (fn [& args]
      (vswap! !task
              (fn [^TimerTask t]
                (when t
                  (.cancel t))
                (let [^TimerTask t (proxy [java.util.TimerTask] []
                                     (run []
                                       (apply f args)))]
                  (.schedule timer t ^long ms)
                  t))))))
