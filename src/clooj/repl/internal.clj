(ns clooj.repl.internal
  (:require
   [clojure.pprint :as pprint]
   [clooj.protocols :as proto])
  (:import
   (java.io Writer)))

(defn start-repl [^Writer result-writer]
  (reify proto/Repl
    (evaluate [this code]
      (.write result-writer
              ^String (with-out-str
                        (pprint/pprint
                         (eval (read-string code))))))))
