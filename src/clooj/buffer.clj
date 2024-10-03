(ns clooj.buffer
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clooj.state :as state])
  (:import
   (java.io File)
   (java.nio.file Files)
   (java.util EnumSet)
   (org.eclipse.jetty.http MimeTypes)
   (org.fife.ui.rsyntaxtextarea RSyntaxDocument SyntaxConstants)))

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

(defn doc-for-file [^File file]
  (let [;; If there's no period in the filename this yields the whole name,
        ;; which works out for Makefile/Dockerfile
        ext (str/lower-case (last (str/split (.getName (io/file "foo.cljs")) #"\.")))
        doc (RSyntaxDocument. (ext->syntax-style ext "text/plain"))]
    (.insertString doc 0 ^String (slurp file) nil)
    doc))

(defn ensure-buffer-for-file [^File file]
  (let [path (.getCanonicalPath file)]
    (get
     (swap! state/buffers
            (fn [buffers]
              (if (get buffers path)
                buffers
                (assoc buffers path {:file file
                                     :doc (doc-for-file file)}))))
     path)))

(comment
  (ensure-buffer-for-file (io/file "/home/arne/github/clooj/src/clooj/buffer.clj")))
