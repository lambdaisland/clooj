(ns clooj.actions
  (:require
   [clooj.analysis.parse-tree :as parse-tree]
   [clooj.document :as doc]
   [clooj.gui :as gui]
   [clooj.repl :as repl]
   [clooj.state :as state]
   [clooj.text-area :as text-area]))

(defn increase-font-size
  "Increase font size"
  [{:keys [comp-id]}]
  (swap! state/component-config update-in [comp-id :font 1] inc))

(defn decrease-font-size
  "Decrease font size"
  [{:keys [comp-id]}]
  (swap! state/component-config update-in [comp-id :font 1] dec))

(defn grow-selection [{:keys [comp-id]}]
  (let [text-comp (gui/resolve comp-id)
        [sel-start sel-end] ((juxt text-area/selection-start text-area/selection-end) text-comp)
        {:keys [parse-tree]} (doc/resolve (:document (gui/config comp-id)))
        overlapping  (take-while #(apply = %)
                                 (map list
                                      (parse-tree/at-pos @parse-tree sel-start)
                                      (parse-tree/at-pos @parse-tree sel-end)))
        {:keys [pos end]} (meta (first (last overlapping)))]
    (when (and pos end)
      (if (and (= pos sel-start) (= (inc end) sel-end))
        (let [{:keys [pos end]} (meta (first (last (butlast overlapping))))]
          (when (and pos end)
            (do
              (text-area/set-selection-start text-comp pos)
              (text-area/set-selection-end text-comp (inc end)))))
        (do
          (text-area/set-selection-start text-comp pos)
          (text-area/set-selection-end text-comp (inc end)))))))

(def default-actions
  {:font-size/increase #'increase-font-size
   :font-size/decrease #'decrease-font-size
   :eval/document      #'repl/eval-document
   :eval/outer-sexp    #'repl/eval-outer-sexp
   :selection/grow     #'grow-selection})
