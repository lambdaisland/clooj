(ns clooj.actions.font-size
  (:require [clooj.state :as state]))

(defn increase [{:keys [comp-id]}]
  (swap! state/component-config update-in [comp-id :font 1] inc))

(defn decrease [{:keys [comp-id]}]
  (swap! state/component-config update-in [comp-id :font 1] dec))
