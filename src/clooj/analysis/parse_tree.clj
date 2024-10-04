(ns clooj.analysis.parse-tree
  (:require
   [clojure.tools.reader.reader-types :as reader-types]
   [clooj.analysis.tools-reader :as tools-reader]
   [clooj.utils :as util])
  (:import
   (java.io CharArrayReader)
   (javax.swing.event DocumentEvent DocumentListener)
   (javax.swing.text Segment)
   (org.fife.ui.rsyntaxtextarea RSyntaxDocument)))

(set! *warn-on-reflection* true)

(defn middle-idx [cnt]
  (long (Math/floor (/ cnt 2))))

(defn lower-half [v idx]
  (subvec v 0 idx))

(defn upper-half [v idx]
  (subvec v idx))

(defn children [el]
  (cond
    (instance? clooj.analysis.tools_reader.MetaNode el)
    [(:m el) (:o el)]
    (instance? clooj.analysis.tools_reader.DiscardNode el)
    [(:o el)]
    (or (instance? clooj.analysis.tools_reader.CommentNode el)
        (instance? clooj.analysis.tools_reader.LiteralNode el))
    [el]
    (sequential? el)
    (vec el)
    (map? el)
    (vec (sort-by (comp :pos meta) (mapcat (juxt key val) el)))
    (set? el)
    (vec (sort-by (comp :pos meta) el))))

(defn at-pos
  "Binary search for form-at-point"
  [forms idx]
  #_(when-not (number? idx)
      (prn "NO NUMBER" idx))
  (let [mid (middle-idx (count forms))
        el (get forms mid)
        {:keys [pos end]} (meta el)]
    (when el
      #_(when-not (number? pos)
          (prn "NO META" el))
      #_(when-not (number? (:pos (meta (first forms))))
          (prn "NO META" (first forms)))
      #_(when-not (number? (:end (meta (last forms))))
          (prn "NO META" (last forms)))
      (cond
        (or (< idx (:pos (meta (first forms))))
            (< (:end (meta (last forms))) idx))
        nil
        (< idx pos)
        (at-pos (lower-half forms mid) idx)
        (< end idx)
        (at-pos (upper-half forms mid) idx)
        (<= pos idx end)
        (if-let [ch (children el)]
          (cons el (at-pos ch idx))
          [el])))))

(defn char-array-reader [arr offset count doc-offset]
  (tools-reader/pos-push-back-reader
   (CharArrayReader. arr offset count)
   doc-offset))

(defn read-parse-tree [rdr]
  (vec (take-while identity (remove #{:error}
                                    (repeatedly #(try
                                                   (tools-reader/read {:eof nil} rdr)
                                                   (catch Exception e
                                                     :error)))))))

(defonce ^Segment segment (Segment.))

(defn reparse-from-offset
  "Partially reparse the document, offset is the point from which changes have
  occured. We retain any top-level forms that end before this point, and then
  parse from after the last retained form."
  [^RSyntaxDocument doc parse-tree offset]
  (vswap! parse-tree
          (fn [pt]
            (let [unchanged (take-while (fn [o]
                                          (< (:end (meta o)) offset))
                                        pt)
                  end-unchanged (or (:end (meta (last unchanged))) 0)
                  doc-length (.getLength doc)
                  diff-len (- doc-length end-unchanged)]
              #_(println "REPARSE"
                         {:ofset offset
                          :end-unchanged end-unchanged
                          :doc-length doc-length
                          :count-unchaged (count unchanged)
                          :count-prev (count pt)
                          :diff-len diff-len})
              (.getText doc end-unchanged diff-len segment)
              (let [new (read-parse-tree (char-array-reader
                                          (.-array segment)
                                          (.-offset segment)
                                          (.-count segment)
                                          end-unchanged))]
                ;; (println "UNCHANGED" unchanged)
                ;; (println (map meta unchanged))
                ;; (println "NEW" new)
                ;; (println (map meta new))
                (into (vec unchanged) new))))))

#_
(let [doc (clooj.text-area/doc (clooj.gui/resolve :doc-text-area))]
  (run!
   #(.removeDocumentListener doc %)
   (filter #(re-find #"parse_tree" (str (class %)))
           (.getDocumentListeners doc))))

;; Experimetally when typing naturally it seems the time between keystrokes for
;; me ranges from 50 to 300ms, so 250 seems reasonable. As soon as a person
;; stops timing for this long we (partially) reparse. (re-)parsing takes less
;; than 1ms on small files, up to tens of milleseconds for larger
;; files (clojure.core which is 200+kb / 8000+lines parses in 50ms), this is on
;; an old thinkpad.
(def debounce-timeout 250)

(def document-parse-tree
  (memoize
   (fn [^RSyntaxDocument doc]
     (let [parse-tree (volatile! nil)
           reparse-from (volatile! (.getLength doc))
           _ (reparse-from-offset doc parse-tree 0)
           reparse-fn (util/debounce (fn []
                                       (vswap! reparse-from
                                               (fn [offset]
                                                 (let [doc-len (.getLength doc)]
                                                   (when (< offset doc-len)
                                                     (reparse-from-offset doc parse-tree offset))
                                                   doc-len))))
                                     debounce-timeout)
           on-change (fn [^DocumentEvent evt]
                       (vswap! reparse-from #(Math/min (long %) (long (.getOffset evt))))
                       (reparse-fn))]
       (.addDocumentListener
        doc
        (reify DocumentListener
          (insertUpdate [this evt] (on-change evt))
          (removeUpdate [this evt] (on-change evt))
          (changedUpdate [this evt]
            ;; "style change"
            #_(on-change evt))))
       parse-tree))))

#_
(.getDocumentListeners
 (clooj.text-area/doc (clooj.gui/resolve :doc-text-area)))
#_
(let [doc (clooj.text-area/doc (clooj.gui/resolve :doc-text-area))]

  (run!
   #(.removeDocumentListener doc %)
   (filter #(re-find #"parse_tree" (str (class %)))
           (.getDocumentListeners doc))))

#_
(doseq [l
        (drop 5
              (.getDocumentListeners
               (clooj.text-area/doc (clooj.gui/resolve :doc-text-area))))]
  (.removeDocumentListener  (clooj.text-area/doc (clooj.gui/resolve :doc-text-area))
                            l))

#_
(def x
  (document-parse-tree (clooj.text-area/doc (clooj.gui/resolve :doc-text-area))))
#_
(last @x)

