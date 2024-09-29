(ns clooj.analysis.parse-tree
  (:require
   [clooj.analysis.tools-reader :as tools-reader]))

(defn middle-idx [cnt]
  (long (Math/floor (/ cnt 2))))

(defn lower-half [v idx]
  (subvec v 0 idx))

(defn upper-half [v idx]
  (subvec v idx))

(defn find-by-pos
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

