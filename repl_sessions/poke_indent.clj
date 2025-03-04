(ns repl-sessions.poke-indent
  (:require
   [casa.squid.acrylic.state :as state]))

(map meta
     @(:parse-tree
       (get @state/documents
            "/home/arne/repos/arnebrasseur.net/src/net/arnebrasseur.clj")))
