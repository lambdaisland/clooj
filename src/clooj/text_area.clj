(ns clooj.text-area
  (:import
   (org.fife.ui.rsyntaxtextarea RSyntaxTextArea
                                RSyntaxDocument)))

(comment
  (clojure.reflect/reflect RSyntaxTextArea)
  (clojure.reflect/reflect (RSyntaxTextArea.))
  (class (caret-position  (RSyntaxTextArea.)))
  (.hasFocus (RSyntaxTextArea.))
  )

(defn caret-position ^long [^RSyntaxTextArea rsta]
  (.getCaretPosition rsta))

(defn text ^String [^RSyntaxTextArea rsta]
  (.getText rsta))

(defn set-text [^RSyntaxTextArea rsta ^String text]
  (.setText rsta text))

(defn doc ^RSyntaxDocument [^RSyntaxTextArea rsta]
  (.getDocument rsta))

(defn replace-str [^RSyntaxTextArea rsta start len new-str]
  (.replace (doc rsta) start len new-str nil))

(defn focus? [^RSyntaxTextArea rsta]
  (.hasFocus rsta))
