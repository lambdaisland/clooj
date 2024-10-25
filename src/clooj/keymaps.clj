(ns clooj.keymaps
  (:require
   [clooj.state :as state]
   [clooj.utils :as util]
   [lambdaisland.data-printers :as data-printers])
  (:import
   (java.awt.event ActionEvent)
   (javax.swing AbstractAction Action ActionMap ComponentInputMap InputMap JComponent KeyStroke)))

;; Swing key handling, as described in KeyboardManager.java. In order these things get a crack and "consuming" a KeyEvent
;; - FocusManager (e.g. TAB to change focus)
;; - focused component itself
;; - component dispatches to event listeners
;; - component dispatches according to inputmap/actionmap
;;   - WHEN_FOCUSED InputMap
;;   - Ancestor chain WHEN_ANCESTOR_OF_FOCUSED_COMPONENT InputMaps
;; - KeyboardManager
;;   - WHEN_IN_FOCUSED_WINDOW of any component
;; - menus

;; In particular in descendants of JTextComponent there is a special
;; InputMap (KeymapWrapper) which intercepts "typed x" events and handles
;; keyboard input into the text area

(defn keystroke [^String s]
  (let [mac? (util/is-mac)]
    (KeyStroke/getKeyStroke
     (-> s
         (.replace "cmd1" (if mac? "meta" "ctrl"))
         (.replace "cmd2" (if mac? "ctrl" "alt"))))))

(data-printers/register-print KeyStroke 'javax.swing/KeyStroke #(.toString ^Object %))

(defn focus-map ^InputMap [^JComponent comp]
  (.getInputMap comp JComponent/WHEN_FOCUSED))

(defn ancestor-map ^InputMap [^JComponent comp]
  (.getInputMap comp JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT))

(defn global-map ^InputMap [^JComponent comp]
  (.getInputMap comp JComponent/WHEN_IN_FOCUSED_WINDOW))

(defn set-focus-map ^InputMap [^JComponent comp ^InputMap m]
  (.setInputMap comp JComponent/WHEN_FOCUSED m))

(defn set-ancestor-map ^InputMap [^JComponent comp ^InputMap m]
  (.setInputMap comp JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT m))

(defn set-global-map ^InputMap [^JComponent comp ^ComponentInputMap m]
  (.setInputMap comp JComponent/WHEN_IN_FOCUSED_WINDOW m))

(defn set-action-map ^ActionMap [^JComponent comp ^ActionMap m]
  (.setActionMap comp m))

(defn action-map ^ActionMap [^JComponent comp]
  (.getActionMap comp))

(defn keymap
  "Coerce to keymap, i.e. values are Keystroke instances. Values should be action
  keys (we use keywords but can be arbitrary objects.)"
  [m]
  (update-keys m #(if (string? %) (keystroke %) %)))

(defn debug [& strs]
  (apply println "[DEBUG]" strs))

(defn fun-input-map
  "Input map backed by a plain function
  - 0 args - list keys
  - 1 arg - do lookup"
  ([f]
   (fun-input-map f nil))
  ([f ^InputMap parent]
   (proxy [InputMap] []
     (allKeys []
       (into-array KeyStroke
                   (concat (f)
                           (when parent
                             (.allKeys parent)))))
     (clear []
       (throw (UnsupportedOperationException. "Can't clear immutable InputMap")))
     (get [keystroke]
       (debug "Keystroke:" keystroke)
       (or (f keystroke)
           (when parent
             (.get parent keystroke))))
     (keys []
       (into-array KeyStroke (f)))
     (put [_ _]
       (throw (UnsupportedOperationException. "Can't insert into immutable InputMap")))
     (remove [_]
       (throw (UnsupportedOperationException. "Can't remove from immutable InputMap")))
     (setParent [_]
       (throw (UnsupportedOperationException. "Can't change parent of immutable InputMap")))
     (size []
       (count (f))))))

(defn fun-action-map
  ([f]
   (fun-action-map f nil))
  ([f ^ActionMap parent]
   (proxy [ActionMap] []
     (allKeys []
       (into-array
        Object
        (concat (f)
                (when parent
                  (.allKeys parent)))))
     (clear []
       (throw (UnsupportedOperationException. "Can't clear immutable ActionMap")))
     (get ^Action [o]
       (debug "Action:" o (f o))
       (if-let [action-fn (f o)]
         (proxy [AbstractAction] []
           (actionPerformed [event]
             (action-fn event)))
         (when parent
           (.get parent o))))
     (keys []
       (into-array Object (f)))
     (put [_ _]
       (throw (UnsupportedOperationException. "Can't insert into immutable ActionMap")))
     (remove [_]
       (throw (UnsupportedOperationException. "Can't remove from immutable ActionMap")))
     (setParent [_]
       (throw (UnsupportedOperationException. "Can't change parent of immutable ActionMap")))
     (size []
       (count (f))))))

(defn setup-keymaps [^JComponent comp comp-id]
  (set-focus-map
   comp
   (fun-input-map
    (fn [keystroke]
      (let [cc @state/component-config]
        (first
         (for [km-key (get-in cc [comp-id :keymaps :focus])
               :let [action-key (get-in @state/keymaps [km-key keystroke])]
               :when action-key]
           action-key))))
    (focus-map comp)))

  (set-action-map
   comp
   (fun-action-map
    (fn [o]
      (let [cc @state/component-config]
        (first
         (for [am-key (get-in cc [comp-id :action-maps])
               :let [action (get-in @state/action-maps [am-key o])]
               :when action]
           (fn [^ActionEvent e]
             (action {:source (.getSource e)
                      :comp-id comp-id
                      :action am-key}))))))
    (action-map comp))))

(def default-keymaps
  {:font-sizing
   (keymap
    {"cmd1 EQUALS" :font-size/increase
     "cmd1 MINUS" :font-size/decrease})

   :eval
   (keymap
    {"shift cmd1 ENTER" :eval/document
     "cmd1 ENTER" :eval/outer-sexp})

   :selection
   (keymap
    {"shift cmd1 UP" :selection/grow})

   :file
   (keymap
    {"cmd1 S" :file/save})})
