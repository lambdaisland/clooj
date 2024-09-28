(ns clooj.repl.internal
  (:require
   [clojure.pprint :as pprint]
   [clooj.protocols :as proto])
  (:import
   (clojure.lang Var)
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
    (capabilities [this]
      #{:eval :ns-info :var-info})
    (evaluate [this code]
      (println "EVAL" code)
      (.write result-writer (eval-str code)))
    (close [this]
      )
    (ns-info [this ns-sym]
      (when-let [ns (the-ns ns-sym)]
        {:name (ns-name ns)
         :aliases (update-vals (ns-aliases ns) ns-name)
         :mappings (update-vals (ns-map ns) (fn [o]
                                              (cond
                                                (var? o)
                                                (.toSymbol ^Var o)
                                                (class? o)
                                                (symbol (.getName ^Class o)))))
         :meta (meta ns)}))
    (var-info [this var-sym]
      (when-let [var (resolve var-sym)]
        (let [{:keys [arglists line column name file ns]} (meta var)]
          {:ns (ns-name ns)
           :name name
           :file file
           :line line
           :column column
           :arglists arglists})))))

(ns-map *ns*)
