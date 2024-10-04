(ns clooj.state)

(defonce component-registry
  (atom {}))

(defonce component-config
  (atom
   {:doc-text-area      {:middleware  {}
                         :font        ["Iosevka Fixed SS14" 19]
                         :line-wrap   false
                         :keymaps     {:focus [:eval :font-sizing]}
                         :action-maps [:default]}
    :repl-out-text-area {:middleware  {}
                         :font        ["Iosevka Fixed SS14" 12]
                         :line-wrap   true
                         :keymaps     {:focus [:font-sizing]}
                         :editable?   false
                         :action-maps [:default]
                         :document    "*Clooj Internal REPL*"}
    :repl-in-text-area  {:middleware  {}
                         :font        ["Iosevka Fixed SS14" 12]
                         :line-wrap   true
                         :keymaps     {:focus [:font-sizing]}
                         :action-maps [:default]}
    :arglist-label      {:font ["Monospaced" 12]}}))

(defonce documents
  (atom {}))

(defonce repls
  (atom {}))

(defonce keymaps
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
  (keys @documents)

  (reset! action-maps

          {:default
           {:font-size/increase (action clooj.font-size/increase)
            :font-size/decrease (action clooj.font-size/decrease)
            :eval/last-sexp (action clooj.repl/eval-outer-sexp)}})

  (:doc-text-area @component-config)

  )
