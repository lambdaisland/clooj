(ns casa.squid.acrylic.cmdline
  "Command line entry point"
  (:require
   [clooj.main :as clooj]
   [lambdaisland.cli :as cli]))

(defn main [opts]
  (clooj/startup))

(def flags [])

(defn -main [& args]
  (cli/dispatch* {:name "acrylic"
                  :flags flags
                  :command #'main}
                 args))
