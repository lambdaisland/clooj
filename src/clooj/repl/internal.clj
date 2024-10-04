(ns clooj.repl.internal
  (:require
   [clojure.pprint :as pprint]
   [clooj.protocols :as proto])
  (:import
   (clojure.lang Var)
   (java.io PushbackReader StringReader Writer)))


(defn eval-str ^String [ns code]
  (try
    (create-ns ns)
    (let [rdr (PushbackReader. (StringReader. code))]
      (with-out-str
        (binding [*ns* (the-ns ns)]
          (loop [form (read {:eof ::done} rdr)]
            (when (not= ::done form)
              (pprint/pprint (eval form))
              (recur (read {:eof ::done} rdr)))))))
    (catch Throwable t
      (with-out-str (println t)))))

(defn start-repl [^Writer result-writer]
  (reify proto/ClojureRuntime
    (capabilities [this]
      #{:eval :ns-info :var-info})
    (evaluate [this ns code]
      (println "EVAL" code)
      (.write result-writer (str "\n" ns "=> " code "\n"))
      (.write result-writer (eval-str ns code)))
    (close [this]
      )
    (ns-info [this ns-sym]
      (when-let [ns (and ns-sym (the-ns ns-sym))]
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
        (let [{:keys [ns] :as m} (meta var)]
          (assoc m :ns (ns-name ns)))))))

