(ns witch.tape-reader
  (:require [clojure.pprint :as pp]))

(def block-markers
  {"#0" :block0
   "#1" :block1
   "#2" :block2
   "#3" :block3
   "#4" :block4
   "#5" :block5
   "#6" :block6
   "#7" :block7
   "#8" :block8
   "#9" :block9
   })

(defn convert-tape-string
  [l]
  (let [m1 (re-find #"^[-+]\d{8}$" l)
        m2 (re-find #"^\d{5}+$" l)
        m3 (re-find #"#\d$" l)]

    (when-not (or m1 m2 m3)
      (throw (Exception. (str "Numerical tape format error:" l))))

    (cond
      m3 (get block-markers m3)
      m2 (/ (bigdec m2) 10000M)
      m1 (/ (bigdec m1) 10000000M))))

(defn process-one-tape
  [s]
  (doall
    (->>
      s
      (map convert-tape-string)
      (concat [:no-block])
      (partition 2 1)
      (remove (comp keyword? second))
      (map
        (fn [[a b]] (if (number? a) [:no-block b] [a b]))))))

(defn comment-or-blank?
  [l]
  (re-matches #"^;.*|\s*" l))

(defn read-tapes
  [args]
  (when-let [filename (first args)]
    (pp/cl-format true "Loading tapes ~a~%" filename)
    (into [] (->>
               (clojure.java.io/reader filename)
               (line-seq)
               (remove comment-or-blank?)
               (partition-by (partial = "==tape"))
               (remove (partial = '("==tape")))
               (map process-one-tape)))))
