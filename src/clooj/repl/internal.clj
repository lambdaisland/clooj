(ns clooj.repl.internal
  (:require
   [clojure.pprint :as pprint]
   [clooj.protocols :as proto])
  (:import
   (java.io Writer)))

(defn eval-str ^String [code]
  (try
    (with-out-str
      (pprint/pprint
       (eval (read-string code))))
    (catch Throwable t
      (with-out-str (println t)))))

(defn start-repl [^Writer result-writer]
  (reify proto/ClojureRuntime
    (evaluate [this code]
      (println "EVAL" code)
      (.write result-writer (eval-str code)))))
