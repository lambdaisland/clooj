(ns clooj.text-area
  (:require
   [clojure.string :as str]
   [clooj.utils :as util])
  (:import
   (java.awt Point)
   (javax.swing JViewport)
   (javax.swing.event CaretListener DocumentListener)
   (org.fife.ui.rsyntaxtextarea AbstractTokenMaker RSyntaxDocument RSyntaxTextArea TokenMakerFactory)))

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

(defn get-line-text [^RSyntaxTextArea text-pane line]
  (let [start (.getLineStartOffset text-pane line)
        length (- (.getLineEndOffset text-pane line) start)]
    (.. text-pane getDocument (getText start length))))

(defn append-text
  ([text-pane text scroll-to-end?]
   (append-text text-pane text))
  ([^RSyntaxTextArea text-pane text]
   (.append text-pane text)))

(defn get-coords [^RSyntaxTextArea text-comp offset]
  (let [row (.getLineOfOffset text-comp offset)
        col (- offset (.getLineStartOffset text-comp row))]
    {:row row :col col}))

(defn get-caret-coords [^RSyntaxTextArea text-comp]
  (get-coords text-comp (.getCaretPosition text-comp)))

(defn add-text-change-listener
  "Executes f whenever text is changed in text component."
  [^RSyntaxTextArea text-comp f]
  (.addDocumentListener
   (.getDocument text-comp)
   (reify DocumentListener
     (insertUpdate [this evt] (f text-comp))
     (removeUpdate [this evt] (f text-comp))
     (changedUpdate [this evt]))))

(defn remove-text-change-listeners [^RSyntaxTextArea text-comp]
  (let [d (.getDocument text-comp)]
    (doseq [l (.getDocumentListeners ^RSyntaxDocument d)]
      (.removeDocumentListener d l))))

(defn get-text-str [^RSyntaxTextArea text-comp]
  (let [^RSyntaxDocument doc (.getDocument text-comp)]
    (.getText doc 0 (.getLength doc))))

(defn add-caret-listener [^RSyntaxTextArea text-comp f]
  (.addCaretListener text-comp
                     (reify CaretListener (caretUpdate [this evt]
                                            (f text-comp)))))

(defn set-selection [^RSyntaxTextArea text-comp start end]
  (doto text-comp (.setSelectionStart start) (.setSelectionEnd end)))

(defn scroll-to-pos [^RSyntaxTextArea text-area offset]
  (let [r (.modelToView text-area offset)
        ^JViewport v (.getParent text-area)
        l (.. v getViewSize height)
        h (.. v getViewRect height)]
    (when r
      (.setViewPosition v
                        (Point. 0 (min (- l h) (max 0 (- (.y r) (/ h 2)))))))))

(defn scroll-to-line [^RSyntaxTextArea text-comp line]
  (let [text (.getText text-comp)
        pos (inc (count (str/join "\n" (take (dec line) (str/split text #"\n")))))]
    (.setCaretPosition text-comp pos)
    (scroll-to-pos text-comp pos)))

(defn scroll-to-caret [^RSyntaxTextArea text-comp]
  (scroll-to-pos text-comp (.getCaretPosition text-comp)))

(defn focus-in-text-component [^RSyntaxTextArea text-comp]
  (.requestFocusInWindow text-comp)
  (scroll-to-caret text-comp))

(defn get-selected-lines [^RSyntaxTextArea text-comp]
  (let [row1 (.getLineOfOffset text-comp (.getSelectionStart text-comp))
        row2 (inc (.getLineOfOffset text-comp (.getSelectionEnd text-comp)))]
    (doall (range row1 row2))))

(defn get-selected-line-starts [^RSyntaxTextArea text-comp]
  (map #(.getLineStartOffset text-comp %)
       (reverse (get-selected-lines text-comp))))

(defn insert-in-selected-row-headers [^RSyntaxTextArea text-comp txt]
  (util/awt-event
   (let [starts (get-selected-line-starts text-comp)
         document (.getDocument text-comp)]
     (dorun (map #(.insertString document % txt nil) starts)))))

(defn remove-from-selected-row-headers [^RSyntaxTextArea text-comp txt]
  (util/awt-event
    (let [len (count txt)
          document (.getDocument text-comp)]
      (doseq [start (get-selected-line-starts text-comp)]
        (when (= (.getText (.getDocument text-comp) start len) txt)
          (.remove document start len))))))

(defn comment-out [^RSyntaxTextArea text-comp]
  (insert-in-selected-row-headers text-comp ";"))

(defn uncomment-out [^RSyntaxTextArea text-comp]
  (remove-from-selected-row-headers text-comp ";"))

(defn toggle-comment [^RSyntaxTextArea text-comp]
  (if (= (.getText (.getDocument text-comp)
                   (first (get-selected-line-starts text-comp)) 1)
         ";")
    (uncomment-out text-comp)
    (comment-out text-comp)))

(defn indent [^RSyntaxTextArea text-comp]
  (when (.isFocusOwner text-comp)
    (insert-in-selected-row-headers text-comp " ")))

(defn unindent [^RSyntaxTextArea text-comp]
  (when (.isFocusOwner text-comp)
    (remove-from-selected-row-headers text-comp " ")))

(def clojure-token-maker
  (delay (.getTokenMaker (TokenMakerFactory/getDefaultInstance) "text/clojure")))

(defn make-rsyntax-text-area ^RSyntaxTextArea []
  (let [^AbstractTokenMaker token-maker @clojure-token-maker
        token-map (.getWordsToHighlight token-maker)
        rsta (proxy [RSyntaxTextArea] []
               (addWordToHighlight [word token-type]
                 (do
                   (.put token-map word token-type)
                   token-type)))
        ^RSyntaxDocument document (.getDocument rsta)]
    (.setTokenMakerFactory document (TokenMakerFactory/getDefaultInstance))
    rsta))

(defn make-text-area ^RSyntaxTextArea [wrap]
  (doto (RSyntaxTextArea.)
    (.setAnimateBracketMatching false)
    (.setBracketMatchingEnabled false)
    (.setAutoIndentEnabled false)
    (.setAntiAliasingEnabled true)
    (.setLineWrap wrap)))
