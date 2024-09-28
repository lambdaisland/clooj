;; Copyright (c) 2011-2013, Arthur Edelstein
;; All rights reserved.
;; Eclipse Public License 1.0
;; arthuredelstein@gmail.com

(ns clooj.brackets
  (:require
   [clojure.string :as str]
   [clooj.utils :as utils]
   [clooj.text-area :as text-area])
  (:import
   (javax.swing.text JTextComponent)))

(set! *warn-on-reflection* true)

(defn mismatched-brackets [a b]
  (and (or (nil? a) (some #{a} [\( \[ \{]))
       (some #{b} [\) \] \}])
       (not (some #{[a b]} [[\( \)] [\[ \]] [\{ \}]]))))

(defn process-bracket-stack
  "Receiving a bracket stack s, deal with the next character c
   and datum dat."
  [s c dat]
  (let [l (ffirst s)        ;last char
        p (next s)          ;pop stack
        j (conj s [c dat])] ;conj [char dat] to stack
    (condp = l
      \\ p
      \" (condp = c, \" p, \\ j, s)
      \; (if (= c \newline) p s)
      (condp = c
        \" j \\ j \; j ;"
        \( j \[ j \{ j
        \) p \] p \} p
        s))))

(defn find-enclosing-brackets [text ^long pos]
  (let [process #(process-bracket-stack %1 %2 nil)
        reckon-dist (fn [stacks]
                      (let [scores (map count stacks)]
                        (utils/count-while #(<= (first scores) %) scores)))
        text-length (Math/min (long (count ^String text)) pos)
        before (subs text 0 text-length)
        stacks-before (reverse (reductions process nil before))
        left (- pos (reckon-dist stacks-before))
        after (subs text text-length)
        stacks-after (reductions process (first stacks-before) after)
        right (+ -1 pos (reckon-dist stacks-after))]
    [left right]))

(defn find-bad-brackets [text]
  (loop [t text pos 0 stack nil errs nil]
    (let [c (first t)        ;this char
          new-stack (process-bracket-stack stack c pos)
          e (when (mismatched-brackets (ffirst stack) c)
              (list (first stack) [c pos]))
          new-errs (if e (concat errs e) errs)]
      (if (next t)
        (recur (next t) (inc pos) new-stack new-errs)
        (filter identity
                (map second (concat new-stack errs)))))))

(defn blank-line-matcher ^java.util.regex.Matcher [s]
  (re-matcher #"[\n\r]\s*?[\n\r]" s))

(defn find-left-gap [text pos]
  (let [p (min (count text) (inc pos))
        before-reverse (str/reverse (subs text 0 p))
        matcher (blank-line-matcher before-reverse)]
    (if (.find matcher)
      (- p (.start matcher))
      0)))

(defn find-right-gap [text pos]
  (let [p (max 0 (dec pos))
        after (subs text p)
        matcher (blank-line-matcher after)]
    (if (.find matcher)
      (+ p (.start matcher))
      (count text))))

(defn find-line-group [text-comp]
  (let [text (utils/get-text-str text-comp)
        pos (text-area/caret-position text-comp)]
    [(find-left-gap text pos) (find-right-gap text pos)]))
