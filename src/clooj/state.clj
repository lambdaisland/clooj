(ns clooj.state)

(defonce component-registry
  (atom {}))

(defonce component-config
  (atom
   {:doc-text-area      {:middleware  {}
                         :font        ["Iosevka Fixed SS14" 19]
                         :line-wrap   false
                         :keymaps     {:focus [:selection :eval :font-sizing]}
                         :action-maps [:default]}
    :repl-out-text-area {:middleware  {}
                         :font        ["Iosevka Fixed SS14" 12]
                         :line-wrap   true
                         :keymaps     {:focus [:selection :font-sizing]}
                         :editable?   false
                         :action-maps [:default]
                         :document    "*Clooj Internal REPL*"}
    :repl-in-text-area  {:middleware  {}
                         :font        ["Iosevka Fixed SS14" 12]
                         :line-wrap   true
                         :keymaps     {:focus [:selection :font-sizing]}
                         :action-maps [:default]}
    :arglist-label      {:font ["Monospaced" 12]}}))

(defonce documents
  (atom {}))

(defonce repls
  (atom {}))

(defonce keymaps
  (atom {}))

(defonce action-maps
  (atom {}))



(comment
  (keys @documents)

  (:doc-text-area @component-config)

  )
