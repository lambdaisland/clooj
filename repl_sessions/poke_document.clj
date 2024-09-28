(ns poke-document
  (:import
   (org.fife.ui.rsyntaxtextarea RSyntaxDocument SyntaxConstants)))

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
