;; Copyright (c) 2011-2013, Arthur Edelstein
;; All rights reserved.
;; Eclipse Public License 1.0
;; arthuredelstein@gmail.com

(ns clooj.protocols)

(defprotocol ClojureRuntime
  (capabilities [this])
  (evaluate [this code] "Evaluate code (a string).")
  (close [this] "Stop the repl instance.")
  (ns-info [this ns])
  (var-info [this ns]))
