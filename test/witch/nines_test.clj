(ns witch.nines-test
  (:require [clojure.test :refer :all]
            [witch.nines :as n]))

(deftest to-nines
  (is (= (n/to-nines 1M)
         1M))

  (is (= (n/to-nines 0.01M)
         0.01M))

  (is (= (n/to-nines -1M)
         98.9999999M))

  (is (= (n/to-nines -0.001M)
         99.9989999M))

  (is (= (n/to-nines -0.0000001M)
         99.9999998M))
  )

(deftest from-nines
  (is (= (n/from-nines 1M)
         1M))

  (is (= (n/from-nines 98.9999999M)
         -1M))

  (is (= (n/from-nines 99.9989999M)
         -0.001M))

  (is (= (n/from-nines 99.9999998M)
         -0.0000001M))
  )

(deftest round-places
  (is (= (n/round-places 0.00000001M 4)
         0.0000M))

  (is (= (n/round-places 0.22222222M 5)
         0.22222M))

  (is (= (n/round-places 99.22222222M 5)
         99.22222M))
  )

(deftest add
  (is (= (n/add (n/to-nines 1M) (n/to-nines 1M))
         (n/to-nines 2M)))

  (is (= (n/add (n/to-nines 1.111M) (n/to-nines 1.222M))
         (n/to-nines 2.333M)))

  (is (= (n/add (n/to-nines -2.0M) (n/to-nines -0.04M))
         (n/to-nines -2.04M)))

  (is (= (n/add (n/to-nines 2.0M) (n/to-nines -3.00004M))
         (n/to-nines -1.00004M)))

  (is (= (n/add 01.0000000M 98.9999999M)
         99.9999999M))

  (is (= (n/add 00.0000000M 99.9999999M)
         99.9999999M))
  )

(deftest subtract
  (is (= (n/subtract (n/to-nines 2M) (n/to-nines 1M))
         (n/to-nines 1M)))

  (is (= (n/subtract (n/to-nines 1.222M) (n/to-nines 1.111M))
         (n/to-nines 0.111M)))

  (is (= (n/subtract (n/to-nines -2.0M) (n/to-nines -0.04M))
         (n/to-nines -1.96M)))

  (is (= (n/subtract (n/to-nines 2.0M) (n/to-nines 3.00004M))
         (n/to-nines -1.00004M)))

  (is (= (n/subtract 01.0000000M 01.0000000M)
         99.9999999M))

  (is (= (n/subtract 00.0000000M 99.9999999M)
         00.0000000M))

  (is (= (n/subtract 00.0000000M 00.0000000M)
         99.9999999M))
  )

(deftest multiply
  (is (= (n/multiply (n/to-nines 2M) (n/to-nines 1M))
         (n/to-nines 2M)))

  (is (= (n/multiply (n/to-nines 1.222M) (n/to-nines 1.111M))
         (n/to-nines 1.357642M)))

  (is (= (n/multiply (n/to-nines -2.0M) (n/to-nines -0.04M))
         (n/to-nines 0.08M)))

  (is (= (n/multiply (n/to-nines 3.0M) (n/to-nines -3.00004M))
         (n/to-nines -9.00012M)))

  (is (= (n/multiply (n/to-nines -2.0M) (n/to-nines 3.00004M))
         (n/to-nines -6.00008M)))

  (is (= (n/multiply 00.0000000M 01.0000000M)
         00.0000000M))

  (is (= (n/multiply 00.0000000M 99.9999999M)
         00.0000000M))

  (is (= (n/multiply 99.9999999M 99.9999999M)
         00.0000000M))
  )

(deftest divide)

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

