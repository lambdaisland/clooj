(ns clooj.buffer
  (:refer-clojure :exclude [resolve])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clooj.analysis.parse-tree :as parse-tree]
   [clooj.state :as state])
  (:import
   (java.io File PrintWriter Writer)
   (org.fife.ui.rsyntaxtextarea RSyntaxDocument RSyntaxTextArea)))

(set! *warn-on-reflection* true)

(def ext->syntax-style
  "Mapping from file extension to SyntaxConstants string."
  {"clj"  "text/clojure"
   "cljs" "text/clojure"
   "cljc" "text/clojure"
   "cljx" "text/clojure"
   "edn"  "text/clojure"

   ;; Rough mapping for the syntax styles that RSyntaxTextarea supports out of
   ;; the box. Most of these are not the actual MIME types even though they look
   ;; like them, e.g. java should be text/x-java, not text/java. Some of the
   ;; extensions will also be wrong or missing, this is a quick and dirty list
   ;; from memory/guesswork.
   "properties"   "text/properties"
   "json"         "text/json"
   "d"            "text/d"
   "sc"           "text/scala"
   "scala"        "text/scala"
   "kt"           "text/kotlin"
   "kotlin"       "text/kotlin"
   "mxml"         "text/mxml"
   "js"           "text/javascript"
   "tex"          "text/latex"
   "latex"        "text/latex"
   "makefile"     "text/makefile"
   "xml"          "text/xml"
   "for"          "text/fortran"
   "f90"          "text/fortran"
   "hosts"        "text/hosts"
   "rust"         "text/rust"
   "vb"           "text/vb"
   "asm"          "text/asm"
   "css"          "text/css"
   "cpp"          "text/cpp"
   "csv"          "text/csv"
   "yml"          "text/yaml"
   "yaml"         "text/yaml"
   "handlebars"   "text/handlebars"
   "java"         "text/java"
   "tcl"          "text/tcl"
   "groovy"       "text/groovy"
   "sas"          "text/sas"
   "pl"           "text/perl"
   "dtd"          "text/dtd"
   "asm6502"      "text/asm6502"
   "ts"           "text/typescript"
   "delphi"       "text/delphi"
   "php"          "text/php"
   "jshintrc"     "text/jshintrc"
   "lua"          "text/lua"
   "rb"           "text/ruby"
   "bat"          "text/bat"
   "less"         "text/less"
   "ini"          "text/ini"
   "dockerfile"   "text/dockerfile"
   "txt"          "text/plain"
   "dart"         "text/dart"
   "cs"           "text/cs"
   "proto"        "text/proto"
   "md"           "text/markdown"
   "lisp"         "text/lisp"
   "xhtml"        "text/html"
   "html"         "text/html"
   "py"           "text/python"
   "go"           "text/golang"
   "sql"          "text/sql"
   "htaccess"     "text/htaccess"
   "c"            "text/c"
   "jsp"          "text/jsp"
   "actionscript" "text/actionscript"
   "unix"         "text/unix"})

(defn resolve [buf-name]
  (get @state/buffers buf-name))

(defn file->syntax-style [^File file]
  (let [;; If there's no period in the filename this yields the whole name,
        ;; which works out for Makefile/Dockerfile
        ext (str/lower-case (last (str/split (.getName (io/file "foo.cljs")) #"\.")))]
    (ext->syntax-style ext "text/plain")))

(defn doc-for-file [^File file syntax-style]
  (let [doc (RSyntaxDocument. syntax-style)]
    (.insertString doc 0 ^String (slurp file) nil)
    doc))

(defn with-parse-tree [{:keys [doc syntax-style] :as buf-opts}]
  (cond-> buf-opts
    (= "text/clojure" syntax-style)
    (assoc :parse-tree (parse-tree/document-parse-tree doc))))

(defn assoc-buffer [buffers id buf-opts]
  (assoc buffers id (-> buf-opts
                        with-parse-tree
                        (assoc :id id))))

(defn ensure-buffer [buf-name syntax-style]
  (get
   (swap! state/buffers
          (fn [buffers]
            (if (get buffers buf-name)
              buffers
              (assoc-buffer
               buffers buf-name
               {:name buf-name
                :syntax-style syntax-style
                :doc (RSyntaxDocument. syntax-style)
                :caret 0}))))
   buf-name))

(defn associate-repl [buf-name repl-name]
  (swap! state/buffers assoc-in [buf-name :repl] repl-name))

(defn ensure-buffer-for-file [^File file]
  (let [path (.getCanonicalPath file)]
    (get
     (swap! state/buffers
            (fn [buffers]
              (if (get buffers path)
                buffers
                (assoc-buffer
                 buffers path
                 (let [syntax-style (file->syntax-style file)]
                   {:name         path
                    :file         file
                    :syntax-style syntax-style
                    :doc          (doc-for-file file syntax-style)
                    :repl         :clooj.repl/internal})))))
     path)))

(defn visit-buffer [^RSyntaxTextArea text-area buf-name]
  (let [{:keys [doc caret]} (resolve buf-name)]
    (when doc
      (.setDocument text-area doc)
      (when caret
        (.setCaretPosition text-area caret)))))

(defn visit-file [^RSyntaxTextArea text-area ^File file]
  (let [{:keys [file doc]} (ensure-buffer-for-file file)]
    (.setDocument text-area doc)))

(defn append-str [^RSyntaxDocument doc ^String str]
  (.insertString doc (.getLength doc) str nil))

(defn buffer-writer [buf-name]
  (PrintWriter.
   (proxy [Writer] []
     (write
       ([ch offset length]
        (when-let [{:keys [doc]} (resolve buf-name)]
          (append-str doc (if (instance? CharSequence ch)
                            ch
                            (String. ^chars ch)))))
       ([t]
        (when-let [{:keys [doc]} (resolve buf-name)]
          (append-str doc
                      (if (int? t)
                        (str (char t))
                        (apply str t))))))
     (flush [])
     (close [] nil))
   true))

(defn parse-tree-at-caret [buf-id]
  (let [{:keys [caret parse-tree repl]} (resolve buf-id)]
    (parse-tree/at-pos @parse-tree caret)))

(comment
  (visit-file
   (clooj.gui/resolve :doc-text-area)
   (io/file "/home/arne/github/clooj/src/clooj/buffer.clj"))
  (ensure-buffer-for-file (io/file "/home/arne/github/clooj/src/clooj/buffer.clj")))
