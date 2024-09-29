(ns clooj.gui
  (:refer-clojure :exclude [resolve])
  (:require
   [clooj.state :as state]
   [clooj.utils :as utils])
  (:import
   (java.awt Font)
   (javax.swing JComponent)
   (org.fife.ui.rsyntaxtextarea RSyntaxTextArea)))

(declare apply-config)

(defn register [comp-id comp]
  (swap! state/component-registry assoc comp-id comp)
  (apply-config comp-id)
  comp)

(defn resolve [comp-id]
  (get @state/component-registry comp-id))

(defmulti apply-config-key (fn [comp-id comp config-key config-val] config-key))
(defmethod apply-config-key :default [_ _ _ _])
(defmethod apply-config-key :font [_ ^JComponent comp _ [font size]]
  (utils/awt-event
    (.setFont comp (Font. font Font/PLAIN size))))

(defmethod apply-config-key :line-wrap [_ ^RSyntaxTextArea comp _ mode]
  (utils/awt-event
    (.setLineWrap comp mode)))

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
