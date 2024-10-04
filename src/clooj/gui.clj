(ns clooj.gui
  (:refer-clojure :exclude [resolve])
  (:require
   [clooj.buffer :as buffer]
   [clooj.keymaps :as keymaps]
   [clooj.state :as state]
   [clooj.text-area :as text-area]
   [clooj.utils :as utils])
  (:import
   (java.awt Font)
   (javax.swing JComponent)
   (org.fife.ui.rsyntaxtextarea RSyntaxTextArea)))

(declare apply-config config)

(defn register [comp-id comp]
  (swap! state/component-registry assoc comp-id comp)
  (apply-config comp-id)
  (when (instance? JComponent comp)
    (keymaps/setup-keymaps comp comp-id))
  (when (instance? RSyntaxTextArea comp)
    (text-area/add-caret-listener
     comp
     (fn [^RSyntaxTextArea rsta]
       (prn "CARET POS" comp-id)
       (when-let [buffer (:buffer (config comp-id))]
         (prn "CARET POS" buffer)
         (swap! state/buffers
                assoc-in [buffer :caret]
                (.getCaretPosition rsta))))))
  comp)

(defn resolve [comp-id]
  (get @state/component-registry comp-id))

(defn config [comp-id]
  (get @state/component-config comp-id))

(defmulti apply-config-key (fn [comp-id comp config-key config-val] config-key))
(defmethod apply-config-key :default [_ _ _ _])

(defmethod apply-config-key :font [_ ^JComponent comp _ [font size]]
  (utils/awt-event
    (.setFont comp (Font. font Font/PLAIN size))))

(defmethod apply-config-key :line-wrap [_ ^RSyntaxTextArea rsta _ mode]
  (utils/awt-event
    (.setLineWrap rsta mode)))

(defmethod apply-config-key :editable? [_ ^RSyntaxTextArea rsta _ editable]
  (utils/awt-event
    (.setEditable rsta editable)))

(defmethod apply-config-key :buffer [_ ^RSyntaxTextArea rsta _ buf-name]
  (utils/awt-event
    (buffer/visit-buffer rsta buf-name)))

(defn apply-config [comp-id]
  (when-let [comp (resolve comp-id)]
    (doseq [[k v] (get @state/component-config comp-id)]
      (apply-config-key comp-id comp k v))))

(defn setup-config-watch []
  (add-watch state/component-config
             ::config->gui
             (fn [k r o n]
               (doseq [comp-id (keys n)
                       :let [old-config (get o comp-id)
                             new-config (get n comp-id)]
                       :when (not= old-config new-config)
                       config-key (keys new-config)
                       :let [old-val (get old-config config-key)
                             new-val (get new-config config-key)]
                       :when (not= old-val new-val)]
                 (when-let [comp (resolve comp-id)]
                   (apply-config-key comp-id comp config-key new-val))))))

(defn switch-buffer [comp-id buf-name]
  (swap! state/component-config assoc-in [comp-id :buffer] buf-name))

(defn visiting-buffer-id [comp-id]
  (get-in @state/component-config [comp-id :buffer]))

(defn visiting-buffer [comp-id]
  (buffer/resolve (visiting-buffer-id comp-id)))
