(ns witch.nines-test
  (:require [clojure.test :refer :all]
            [witch.nines :as n]))

(deftest to-nines
  (is (= (n/to-nines 1.0000000M)
         1.00000000M))

  (is (= (n/to-nines 0.0100000M)
         0.01000000M))

  (is (= (n/to-nines -1.0000000M)
         98.9999999M))

  (is (= (n/to-nines -0.0010000M)
         99.9989999M))

  (is (= (n/to-nines -0.0000001M)
         99.9999998M))
  )

(deftest from-nines
  (is (= (n/from-nines 1.0000000M)
         1M))

  (is (= (n/from-nines 98.9999999M)
         -1M))

  (is (= (n/from-nines 99.9989999M)
         -0.001M))

  (is (= (n/from-nines 99.9999998M)
         -0.0000001M))
  )

(deftest round-places
  (is (= (n/adjust-places 0.00000001M 4)
         0.0000M))

  (is (= (n/adjust-places 0.22222222M 5)
         0.22222M))

  (is (= (n/adjust-places 99.22222222M 5)
         99.22222M))

  (is (= (n/adjust-places 9999.222222222222M 5)
         99.22222M))
  )


(deftest examine-sign
  (is (= (n/positive? (n/to-nines 1M))
         true))

  (is (= (n/positive? (n/to-nines -1M))
         false))

  (is (= (n/positive? 00.00000000)
         true))

  (is (= (n/positive? 99.99999999)
         false))
  )

(deftest negate
  (is (= (n/negate 1.111M)
         98.888M))
  (is (= (n/negate 1.1111111M)
         98.8888888M))

  (is (= (n/negate 2.22222222222222M)
         97.77777777777777M))
  )

(deftest carry-over
  (is (= (n/carry-over 100.000000M 2 7)
         00.0000001M))

  (is (= (n/carry-over 100.0000000000000M 2 14)
         00.00000000000001M))

  (is (= (n/carry-over 23400.0000000000000M 2 14)
         00.00000000000234M))

  (is (= (n/carry-over 199.99999999999998M 2 14)
         99.99999999999999M))
  )

(deftest pow10
  (is (= (n/pow10 0M)
         1.0M))
  (is (= (n/pow10 1M)
         10.0M))

  (is (= (n/pow10 5M)
         100000.0M))

  (is (= (n/pow10 -5M)
         0.00001M))
  )