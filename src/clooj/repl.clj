(ns clooj.repl
  (:refer-clojure :exclude [resolve])
  (:require
   [clooj.buffer :as buffer]
   [clooj.gui :as gui]
   [clooj.protocols :as proto]
   [clooj.repl.internal :as internal]
   [clooj.state :as state]
   [clooj.text-area :as text-area]))

(def internal-repl-buffer-name "*Clooj Internal REPL*")

(defn register [identifier repl]
  (swap! state/repls assoc identifier repl))

(defn resolve [identifier]
  (get @state/repls identifier))

(defn start-internal-repl []
  (buffer/ensure-buffer internal-repl-buffer-name "text/clojure")
  (register ::internal
            {:repl (internal/start-repl (buffer/buffer-writer internal-repl-buffer-name))
             :buffer internal-repl-buffer-name}))

(defn comp-id->repl [comp-id]
  (resolve (:repl (gui/visiting-buffer comp-id))))

(defn eval-outer-sexp [{:keys [comp-id] :as o}]
  (let [buf-id (gui/visiting-buffer-id comp-id)
        parse-tree (buffer/parse-tree-at-caret buf-id)
        {:keys [pos end]} (meta (first parse-tree))
        repl (comp-id->repl comp-id)]
    (proto/evaluate (:repl repl)
                    (buffer/buffer-ns buf-id)
                    (text-area/get-text-str (gui/resolve comp-id)
                                            pos (inc (- end pos))))))


(comment
  (let [{:keys [comp-id]} o]
    (comp-id->repl comp-id))

  (start-internal-repl)

  (resolve ::internal)

  (proto/evaluate (:repl (resolve ::internal))
                  "(+ 1 1)"))
