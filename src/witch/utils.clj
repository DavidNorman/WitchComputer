(ns witch.utils)

(defn apply-while
  "Apply a function to a value repeatedly while a predicate
  returns true for the result."
  [f x p]
  (if (p x) (recur f (f x) p) x))

(defn xor
  [& args]
  (if (= 1 (apply bit-xor (map #(if % 1 0) args)))
    true
    false))
