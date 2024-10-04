(ns clooj.repl
  (:refer-clojure :exclude [resolve])
  (:require
   [clooj.document :as document]
   [clooj.gui :as gui]
   [clooj.protocols :as proto]
   [clooj.repl.internal :as internal]
   [clooj.state :as state]
   [clooj.text-area :as text-area]))

(def internal-repl-document-name "*Clooj Internal REPL*")

(defn register [identifier repl]
  (swap! state/repls assoc identifier repl))

(defn resolve [identifier]
  (get @state/repls identifier))

(defn start-internal-repl []
  (document/ensure-document internal-repl-document-name "text/clojure")
  (register ::internal
            {:repl (internal/start-repl (document/document-writer internal-repl-document-name))
             :document internal-repl-document-name}))

(defn comp-id->repl [comp-id]
  (resolve (:repl (gui/visiting-document comp-id))))

(defn eval-outer-sexp [{:keys [comp-id] :as o}]
  (let [buf-id (gui/visiting-document-id comp-id)
        parse-tree (document/parse-tree-at-caret buf-id)
        {:keys [pos end]} (meta (first parse-tree))
        repl (comp-id->repl comp-id)]
    (proto/evaluate (:repl repl)
                    (document/document-ns buf-id)
                    (text-area/get-text-str (gui/resolve comp-id)
                                            pos (inc (- end pos))))))


(comment
  (let [{:keys [comp-id]} o]
    (comp-id->repl comp-id))

  (start-internal-repl)

  (resolve ::internal)

  (proto/evaluate (:repl (resolve ::internal))
                  "(+ 1 1)"))
