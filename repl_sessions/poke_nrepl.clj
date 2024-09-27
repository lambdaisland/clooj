(ns poke-nrepl
  (:require
   [nrepl.core :as nrepl]
   [nrepl.transport :as transport]))

(def client
  (nrepl/client (nrepl/connect
                 :host "localhost"
                 :port 41833
                 :transport-fn #'transport/bencode
                 )
                1000))
(nrepl/message
 client
 {:id "1"
  :op "eval"
  :code "(+ 1 1)"})

