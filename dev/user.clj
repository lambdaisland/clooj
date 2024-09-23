(ns user)

(defn go []
  ((requiring-resolve 'clooj.main/startup)))
