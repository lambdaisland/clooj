(ns casa.squid.acrylic.middleware
  "Document middleware, works on operations coming from the documentfilter."
  (:require
   [casa.squid.acrylic.gui :as gui]
   [casa.squid.acrylic.state :as state]
   [casa.squid.acrylic.text-area :as text-area]))

(defn wrap-match-pair [replace comp-id]
  (fn [offset len text attrs]
    (replace offset len text attrs)
    (if-let [match ({"(" ")" "{" "}" "[" "]"} text)]
      (let [rsta (gui/resolve :doc-text-area)
            pos (text-area/caret-position rsta)]
        (replace pos len match attrs)
        (text-area/set-caret-position rsta pos)))))

(defn debug-mw [msg f]
  (fn [& args]
    (apply println msg args)
    (apply f args)))

(defn enable-middleware [component type var]
  (swap! state/component-config
         update-in [component :middleware type]
         (fn [mw]
           (if (some #{var} mw)
             mw
             ((fnil conj []) mw var)))))

(defn remove-middleware [component type var]
  (swap! state/component-config
         update-in [component :middleware type]
         (partial filterv
                  (complement #{var}))))


#_
(defonce add-match-mw
  (add-middleware :doc-text-area :insert #'match-on-insert)
  )
