;; Copyright (c) 2011-2013, Arthur Edelstein
;; All rights reserved.
;; Eclipse Public License 1.0
;; arthuredelstein@gmail.com

(ns clooj.repl.external
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clooj.clj-inspector.jars :as jars]
   [clooj.protocols :as protocols]
   [clooj.utils :as utils])
  (:import
   (java.io File PrintWriter Writer)
   (java.lang Process)
   (java.net URL URLDecoder)
   (java.util List)
   (java.util.zip ZipEntry)))

(set! *warn-on-reflection* true)

(defn own-clojure-jar
  "Locate the clojure jar being used by clooj (last resort)."
  []
  (when-let [url (io/resource "clojure/lang/RT.class")]
    (-> url .getFile URL. .getFile URLDecoder/decode (str/split #"!/") first)))

(defn jar-contains-class?
  "Does the jar contain a particular class file? Specify the
   classname in a string, e.g. \"clojure.lang.RT\""
  [jar classname]
  (let [entries (jars/get-entries-in-jar jar)
        filenames (map #(.getName ^ZipEntry %) entries)
        desired (str (str/replace classname #"\." "/") ".class")]
    (not (nil? (some #(= % desired) filenames)))))

(defn clojure-jar-location
  "Find the location of a clojure jar in a project."
  [^String project-path]
  (let [lib-dir (str project-path "/lib")
        jars (filter #(.contains (.getName ^File %) "clojure")
                     (jars/jar-files lib-dir))]
    (first (filter #(jar-contains-class? % "clojure.lang.RT") jars))))

(defn repl-classpath-items
  "Figures out the necessary pieces for a viable classpath
   given a particular project directory."
  [project-path]
  (try
    #_(lein/lein-classpath-items project-path)
    (catch Exception e
      [(or (clojure-jar-location project-path)
           #_(own-clojure-jar))
       (str project-path "/lib/*")
       (str project-path "/src")])))

(defn java-binary
  "Returns the fully-qualified path of the java binary."
  []
  (str (System/getProperty "java.home")
       File/separator "bin" File/separator "java"))

(defn repl-process
  "Start an external repl process by running clojure.main."
  ^Process [project-path classpath-items]
  (let [classpath-str (apply str (interpose File/pathSeparatorChar classpath-items))]
    (.start
     (doto (ProcessBuilder. ^String/1 (into-array String [(java-binary) "-cp" classpath-str "clojure.main"]))
       (.redirectErrorStream true)
       (.directory (io/file (or project-path ".")))))))

(defn launch-repl
  "Launch an outside process with a clojure repl."
  [project-path classpath-items result-writer]
  (let [process (repl-process project-path classpath-items)
        input-writer  (PrintWriter. (.getOutputStream process) true)
        is (.getInputStream process)]
    (future (utils/copy-input-stream-to-writer is result-writer)); :buffer-size 10))
    {:input-writer input-writer
     :project-path project-path
     :process process
     :classpath classpath-items
     :result-writer result-writer}))

(defn evaluate-code
  "Evaluate some code in the repl specified by repl-map."
  [repl-map code]
  (binding [*out* (:input-writer repl-map)]
    (println code)
    (.flush *out*)))

(defn close
  "Close the repl specified in the repl-map."
  [{:keys [^Writer input-writer ^Writer result-writer ^Process process] :as repl-map}]
  (doto input-writer .flush .close)
  (.flush result-writer)
  (.destroy process))

(defn repl
  "Returns a repl, based at project-path, where outputs
   are printed to result-writer."
  [project-path classpath-items result-writer]
  (let [repl-map (launch-repl project-path classpath-items result-writer)]
    (reify protocols/ClojureRuntime
      (evaluate [this code]
        (evaluate-code repl-map code))
      (close [this]
        (close repl-map))
      (toString [this]
        (str "Repl with classpath" (:classpath repl-map))))))




