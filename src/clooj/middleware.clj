(ns clooj.middleware
  (:require
   [clooj.state :as state])
  (:import
   (javax.swing.text DocumentFilter DocumentFilter$FilterBypass)))

(defn doc-filter [comp-id]
  (proxy [DocumentFilter] []
    (replace [^DocumentFilter$FilterBypass fb offset len text attrs]
      (let [mw (get-in @state/component-config [comp-id :middleware :replace])
            f (fn [offset len text attrs]
                (.replace fb offset len text attrs))
            f (reduce (fn [f m] (m f)) f mw)]
        (f offset len text attrs)))
    (remove [^DocumentFilter$FilterBypass fb offset len]
      (let [mw (get-in @state/component-config [comp-id :middleware :remove])
            f (fn [offset len]
                (.remove fb offset len))
            f (reduce (fn [f m] (m f)) f mw)]
        (f offset len)))
    (insertString [^DocumentFilter$FilterBypass fb offset string attrs]
      (let [mw (get-in @state/component-config [comp-id :middleware :insert])
            f (fn [offset string attrs]
                (.insertString fb offset string attrs))
            f (reduce (fn [f m] (m f)) f mw)]
        (f offset string attrs)))))

(defn match-on-insert [insert]
  (fn [offset string attrs]
    ;; naive as a proof of concept
    (def o offset)
    (def string string)
    (def attrs attrs)
    (case string
      "{"
      (insert offset "{}" attrs)
      "["
      (insert offset "[]" attrs)
      "("
      (insert offset string attrs)
      nil)))

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
