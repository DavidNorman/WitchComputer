(ns witch.utils-test
  (:require [clojure.test :refer :all]
            [witch.utils :as u]))

; Utility functions

(deftest apply-while
  (is (= (u/apply-while inc 0 (fn [x] (< x 10)))
         10))

  (is (= (u/apply-while (partial cons 1) [] (fn [x] (< (count x) 4)))
         [1 1 1 1]))

  )

(deftest xor
  (is (= (u/xor true true)
         false))

  (is (= (u/xor true false)
         true))

  (is (= (u/xor false true)
         true))

  (is (= (u/xor false false)
         false))

  (is (= (u/xor true true true)
         true))

  (is (= (u/xor true true false)
         false))
  )
