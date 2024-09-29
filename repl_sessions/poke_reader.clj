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

