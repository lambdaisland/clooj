(ns poke-reader
  (:require
   [clojure.tools.reader :as reader]
   [clojure.tools.reader.reader-types
    :as
    reader-types
    :refer
    [IndexingReader]]))

(def src (slurp "/home/arne/github/clojure/src/clj/clojure/core.clj"))

(def rdr (clooj.tools-reader/pos-push-back-string-reader "(foo) ("))

(def forms
  (time
   (vec
    (doall
     (take-while identity
                 (repeatedly
                  #(clooj.tools-reader/read {:eof nil} rdr)
                  ))))))


(defn middle-idx [cnt]
  (long (Math/floor (/ cnt 2))))

(defn lower-half [v idx]
  (subvec v 0 idx))

(defn upper-half [v idx]
  (subvec v idx))

(defn find-by-pos [forms idx]
  (when-not (number? idx)
    (prn "NO NUMBER" idx))
  (let [mid (middle-idx (count forms))
        el (get forms mid)
        {:keys [pos end]} (meta el)]
    (when el
      (when-not (number? pos)
        (prn "NO META" el))
      (when-not (number? (:pos (meta (first forms))))
        (prn "NO META" (first forms)))
      (when-not (number? (:end (meta (last forms))))
        (prn "NO META" (last forms)))
      (cond
        (or (< idx (:pos (meta (first forms))))
            (< (:end (meta (last forms))) idx))
        nil
        (< idx pos)
        (find-by-pos (lower-half forms mid) idx)
        (< end idx)
        (find-by-pos (upper-half forms mid) idx)
        (<= pos idx end)
        (cond
          (instance? clooj.tools_reader.MetaNode el)
          (cons el (find-by-pos [(:m el) (:o el)] idx))
          (instance? clooj.tools_reader.DiscardNode el)
          (cons el (find-by-pos [(:o el)] idx))
          (or (instance? clooj.tools_reader.KeywordNode el)
              (instance? clooj.tools_reader.CommentNode el)
              (instance? clooj.tools_reader.StringNode el)
              (instance? clooj.tools_reader.LiteralNode el))
          [el]
          (sequential? el)
          (cons el (find-by-pos (vec el) idx))
          (map? el)
          (cons el (find-by-pos (vec (sort-by (comp :pos meta) (mapcat (juxt key val) el))) idx))
          (set? el)
          (cons el (find-by-pos (vec (sort-by (comp :pos meta) el)) idx))
          :else
          [el])))))
