(ns clooj.cmdline
  (:require
   [clooj.main :as clooj]
   [lambdaisland.cli :as cli]))

(defn main [opts]
  (clooj/startup))

(def flags [])

(defn -main [& args]
  (cli/dispatch* {:name "clooj"
                  :flags flags
                  :command #'main}
                 args))

