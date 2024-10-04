(ns clooj.state)

(defonce component-registry
  (atom {}))

(defonce component-config
  (atom
   {:doc-text-area {:middleware {}
                    :keymaps []
                    :font ["Iosevka Fixed SS14" 19]
                    :line-wrap false
                    :key-maps {:focus [:eval :font-sizing]}
                    :action-maps [:default]}
    :repl-out-text-area {:middleware {}
                         :keymaps []
                         :font ["Iosevka Fixed SS14" 12]
                         :line-wrap true
                         :key-maps {:focus [:font-sizing]}
                         :editable? false
                         :action-maps [:default]
                         :buffer  "*Clooj Internal REPL*"}
    :repl-in-text-area {:middleware {}
                        :keymaps []
                        :font ["Iosevka Fixed SS14" 12]
                        :line-wrap true
                        :key-maps {:focus [:font-sizing]}
                        :action-maps [:default]}
    :arglist-label {:font ["Monospaced" 12]}}))

(defonce buffers
  (atom {}))

(defonce repls
  (atom {}))

(defonce key-maps
  (atom {}))

(defmacro action [var-name]
  `(fn [o#]
     ((requiring-resolve '~var-name) o#)))

(defonce action-maps
  (atom
   {:default
    {:font-size/increase (action clooj.font-size/increase)
     :font-size/decrease (action clooj.font-size/decrease)
     :eval/last-sexp (action clooj.repl/eval-outer-sexp)}}))



(comment
  (keys @buffers)

  (reset! action-maps

          {:default
           {:font-size/increase (action clooj.font-size/increase)
            :font-size/decrease (action clooj.font-size/decrease)
            :eval/last-sexp (action clooj.repl/eval-outer-sexp)}})

  (:doc-text-area @component-config)

  )
