(ns clooj.keymaps
  (:require
   [lambdaisland.data-printers :as data-printers])
  (:import
   (javax.swing AbstractAction Action InputMap JComponent KeyStroke)))

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
  (KeyStroke/getKeyStroke s))

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

(defn input-map
  ([m]
   (input-map m nil))
  ([m ^InputMap parent]
   (let [m (update-keys m #(if (string? %) (keystroke %) %))]
     (proxy [InputMap java.util.Map clojure.lang.IMeta] []
       (allKeys []
         (into-array KeyStroke
                     (concat (keys m)
                             (when parent
                               (.allKeys parent)))))
       (clear []
         (throw (UnsupportedOperationException. "Can't clear immutable InputMap")))
       (get [keystroke]
         (println keystroke)
         (or (get m keystroke)
             (when parent
               (.get parent keystroke))))
       (keys []
         (into-array KeyStroke (keys m)))
       (put [_ _]
         (throw (UnsupportedOperationException. "Can't insert into immutable InputMap")))
       (remove [_]
         (throw (UnsupportedOperationException. "Can't remove from immutable InputMap")))
       (setParent [_]
         (throw (UnsupportedOperationException. "Can't change parent of immutable InputMap")))
       (size []
         (count m))

       ;; java.util.Map
       (entrySet []
         (.entrySet m))

       ;; clojure.lang.IMeta
       (meta []
         {:entries m
          :parent parent})))))

(defn action-map
  ([m]
   (action-map m nil))
  ([m ^ActionMap parent]
   (proxy [ActionMap clojure.lang.IMeta] []
     (allKeys []
       (into-array
        Object
        (concat (keys m)
                (when parent
                  (.allKeys parent)))))
     (clear []
       (throw (UnsupportedOperationException. "Can't clear immutable ActionMap")))
     (get ^Action [o]
       (or (get m o)
           (when parent
             (.get parent o))))
     (keys []
       (into-array Object (keys m)))
     (put [_ _]
       (throw (UnsupportedOperationException. "Can't insert into immutable ActionMap")))
     (remove [_]
       (throw (UnsupportedOperationException. "Can't remove from immutable ActionMap")))
     (setParent [_]
       (throw (UnsupportedOperationException. "Can't change parent of immutable ActionMap")))
     (size []
       (count m))

     ;; clojure.lang.IMeta
     (meta []
       {:entries m
        :parent parent}))))

;; (def text-area (@clooj.main/current-app :repl-in-text-area))
;; (def default-map (focus-map text-area))

;; (.get (.getInputMap (.getParent (.getParent text-area))) (keystroke "X"))

;; (for [c
;;       (take-while identity
;;                   (iterate #(.getParent %) text-area))]
;;   (try
;;     (.get (.getInputMap c) (keystroke "X"))
;;     (catch Exception e
;;       :error)
;;     )
;;   )
;; (for [m
;;       (take-while identity
;;                   (iterate #(.getParent %) default-map))]
;;   (try
;;     (seq (.keys m))
;;     (catch Exception e
;;       :error)
;;     )
;;   )

;; (def maps (take-while identity
;;                       (iterate #(.getParent %) default-map)))

;; (.size (.getParent default-map))

;; (seq
;;  (.keys default-map))

;; (defn noisy-input-map [i]
;;   (proxy [InputMap] []
;;     (allKeys []
;;       (println "allKeys" i)
;;       (.allKeys i))
;;     (clear []
;;       (println "clear" i)
;;       (.clear i))
;;     (get [keystroke]
;;       (println "get" i keystroke)
;;       (.get i keystroke))
;;     (keys []
;;       (println "keys" i)
;;       (.keys i))
;;     (put [k o]
;;       (println "put" i k o)
;;       (.put i k o))
;;     (remove [k]
;;       (println "remove" i k)
;;       (.remove i k))
;;     (setParent [j]
;;       (println "setParent" i j)
;;       (.setParent i j))
;;     (size []
;;       (println "size" i)
;;       (.size i))
;;     ))

;; (set-focus-map text-area (input-map {"typed x" :XXX}
;;                                     (input-map {}
;;                                                (noisy-input-map
;;                                                 (.getParent default-map)))))

;; (.put
;;  (.getActionMap text-area)
;;  :XXX
;;  (proxy [AbstractAction] []
;;    (actionPerformed [event]
;;      (println "DONE XXX")
;;      (def event event))))




