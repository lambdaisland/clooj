(ns clooj.state)

(def component-registry (atom {}))

(def component-config
  (atom
   {:frame {:keymaps {:global [:font-sizing]}}
    :doc-text-area {:middleware {}
                    :keymaps []
                    :font ["Iosevka Fixed SS14" 19]
                    :line-wrap false}
    :repl-in-text-area {:middleware {}
                        :keymaps []
                        :font ["Iosevka Fixed SS14" 12]
                        :line-wrap true}}))

(def buffers (atom {}))

(def repls (atom {}))

(defonce keymaps
  (atom
   {:font-sizing {}}))

(defonce actions
  (atom {}))
