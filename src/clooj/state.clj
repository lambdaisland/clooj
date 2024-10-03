(ns clooj.state)

(defonce component-registry
  (atom {}))

(defonce component-config
  (atom
   {:doc-text-area {:middleware {}
                    :keymaps []
                    :font ["Iosevka Fixed SS14" 19]
                    :line-wrap false
                    :key-maps {:focus [:font-sizing]}
                    :action-maps [:default]}
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

(defonce action-maps
  (atom
   {:default
    {:font-size/increase #((requiring-resolve 'clooj.font-size/increase) %)
     :font-size/decrease #((requiring-resolve 'clooj.font-size/decrease) %)}}))


