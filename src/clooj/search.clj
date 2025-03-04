;; Copyright (c) 2011-2013, Arthur Edelstein
;; All rights reserved.
;; Eclipse Public License 1.0
;; arthuredelstein@gmail.com

(ns clooj.search
  (:require
   [clooj.highlighting :as highlighting]
   [casa.squid.acrylic.text-area :as text-area]
   [clooj.utils :as utils])
  (:import
   (java.awt Color)
   (java.util.regex Pattern)
   (javax.swing JButton JCheckBox JLabel)
   (org.fife.ui.rsyntaxtextarea RSyntaxTextArea)))

(set! *warn-on-reflection* true)

(defn configure-search [match-case use-regex]
  (bit-or Pattern/CANON_EQ
          Pattern/UNICODE_CASE
          (if match-case 0 Pattern/CASE_INSENSITIVE)
          (if use-regex 0 Pattern/LITERAL)))

(defn find-all-in-string
  [s t match-case use-regex]
  (try
    (when (pos? (count t))
      (let [p (Pattern/compile t (configure-search match-case use-regex))
            m (re-matcher p s)]
        (loop [positions []]
          (if (.find m)
            (recur (conj positions [(.start m) (.end m)] ) )
            positions))))
    (catch Exception _ [])))

(defn highlight-found [text-comp posns]
  (doall
   (map #(highlighting/highlight text-comp (first %) (second %) Color/YELLOW)
        posns)))

(defn next-item [cur-pos posns]
  (or (first (drop-while #(> cur-pos (first %)) posns)) (first posns)))

(defn prev-item [cur-pos posns]
  (or (first (drop-while #(< cur-pos (first %)) (reverse posns))) (last posns)))

(def search-highlights (atom nil))

(def current-pos (atom 0))

(defn update-find-highlight [^RSyntaxTextArea sta app back]
  (let [^RSyntaxTextArea dta (:doc-text-area app)
        match-case (.isSelected ^JCheckBox (:search-match-case-checkbox app))
        use-regex (.isSelected ^JCheckBox (:search-regex-checkbox app))
        posns (find-all-in-string (text-area/get-text-str dta)
                                  (text-area/get-text-str sta)
                                  match-case
                                  use-regex)]
    (highlighting/remove-highlights dta @search-highlights)
    (if (pos? (count posns))
      (let [selected-pos
            (if back (prev-item (dec @current-pos) posns)
                (next-item @current-pos posns))
            posns (remove #(= selected-pos %) posns)
            pos-start (first selected-pos)
            pos-end (second selected-pos)]
        (.setBackground sta Color/WHITE)
        (doto dta
          (.setSelectionStart pos-end)
          (.setSelectionEnd pos-end))
        (reset! current-pos pos-start)
        (reset! search-highlights
                (conj (highlight-found dta posns)
                      (highlighting/highlight dta pos-start
                                              pos-end (.getSelectionColor dta))))
        (text-area/scroll-to-pos dta pos-start))
      (.setBackground sta  Color/PINK))))

(defn start-find [app]
  (let [^RSyntaxTextArea sta (:search-text-area app)
        ^RSyntaxTextArea dta (:doc-text-area app)
        ^JCheckBox case-checkbox (:search-match-case-checkbox app)
        ^JCheckBox regex-checkbox (:search-regex-checkbox app)
        ^JButton close-button (:search-close-button app)
        ^JLabel arg (:arglist-label app)
        sel-text (.getSelectedText dta)]
    (.setVisible arg false)
    (doto sta
      (.setVisible true)
      (.requestFocus)
      (.selectAll))
    (.setVisible case-checkbox true)
    (.setVisible regex-checkbox true)
    (.setVisible close-button true)
    (when (seq sel-text)
      (.setText sta sel-text))))

(defn stop-find [app]
  (let [^RSyntaxTextArea sta (app :search-text-area)
        ^RSyntaxTextArea dta (app :doc-text-area)
        ^JCheckBox case-checkbox (:search-match-case-checkbox app)
        ^JCheckBox regex-checkbox (:search-regex-checkbox app)
        ^JButton close-button (:search-close-button app)
        ^JLabel arg (:arglist-label app)]
    (.setVisible arg true)
    (.setVisible sta false)
    (.setVisible case-checkbox false)
    (.setVisible regex-checkbox false)
    (.setVisible close-button false)
    (highlighting/remove-highlights dta @search-highlights)
    (reset! search-highlights nil)
    (reset! current-pos 0)))

(defn escape-find [app]
  (stop-find app)
  (.requestFocus ^RSyntaxTextArea (:doc-text-area app)))

(defn highlight-step [app back]
  (let [doc-text-area (:doc-text-area app)
        search-text-area (:search-text-area app)]
    (start-find app)
    (when-not back
      (swap! current-pos inc))
    (update-find-highlight search-text-area app back)))
