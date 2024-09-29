(ns clooj.state)

(def components
  (atom
   {:doc-text-area {:middleware {}
                    :keymaps []}
    :repl-in-text-area {:middleware {}
                        :keymaps []}}))

(defonce keymaps
  (atom
   {}))
