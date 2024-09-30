(ns poke-document
  (:require
   [clooj.state :as state])
  (:import
   (org.fife.ui.rsyntaxtextarea SyntaxConstants)))

(.setDocument
 (:doc-text-area @clooj.main/current-app)

 x
 #_(doto (RSyntaxDocument. "text/clojure")
     (.insertString

      0
      "(def hello \"world2\")"
      nil)))

(def x (.getDocument
        (:doc-text-area @clooj.main/current-app)
        ))

(.getText x
          0
          (.getOffset
           (.getEndPosition x)))

(.getCaretPosition x)

SyntaxConstants/SYNTAX_STYLE_CLOJURE

(defn wrap-debug-insert [f]
  (fn [offset string attrs]
    (def offset offset)
    (def string string)
    (def attrs attrs)
    (prn "INSERT" offset string attrs)
    (f offset string attrs)))

(defn wrap-match-pair [f]
  (fn [offset len text attrs]
    (f offset len text attrs)
    (if-let [match ({"(" ")" "{" "}" "[" "]"} text)]
      (let [rsta (clooj.gui/resolve :doc-text-area)
            pos (clooj.text-area/caret-position rsta)]
        (f pos len match attrs)
        (clooj.text-area/set-caret-position rsta pos)))))

(swap! state/component-config assoc-in
       [:doc-text-area :middleware :replace]
       [#'wrap-debug-replace])
(swap! state/component-config assoc-in
       [:doc-text-area :middleware :insert]
       [#'wrap-debug])
