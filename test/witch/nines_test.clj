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

  (is (= (n/add 2.00000000000000M (n/to-nines -3.00004M))
         98.99995990000000M)) ; TODO is this right?
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

  (is (= (n/subtract 2.00000000000000M (n/to-nines 3.00004M))
         98.99995990000000M)) ; TODO is this right?
  )

(deftest multiply
  (is (= (n/multiply (n/to-nines 2M) (n/to-nines 1M) 0M)
         (n/to-nines 2M)))

  (is (= (n/multiply (n/to-nines 1.222M) (n/to-nines 1.111M) 0M)
         (n/to-nines 1.357642M)))

  (is (= (n/multiply (n/to-nines -2.0M) (n/to-nines -0.04M) 0M)
         (n/to-nines 0.08M)))

  (is (= (n/multiply (n/to-nines 3.0M) (n/to-nines -3.00004M) 0M)
         (n/to-nines -9.00012M)))

  (is (= (n/multiply (n/to-nines -2.0M) (n/to-nines 3.00004M) 0M)
         (n/to-nines -6.00008M)))

  (is (= (n/multiply 00.0000000M 01.0000000M 0M)
         00.0000000M))

  (is (= (n/multiply 00.0000000M 99.9999999M 0M)
         00.0000000M))

  (is (= (n/multiply 99.9999999M 99.9999999M 0M)
         00.0000000M))

  ; TODO add cases where accumulator is non zero to start
  )

(deftest divide

  ;;TODO remainders, positive zero dividend (and negative zero?)

  (is (= (n/divide 01.0000000M 01.0000000M)
         01.0000000M))
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

(deftest shift
  (is (= (n/shift (n/to-nines 1M) 1M)
         (n/to-nines 1M)))

  (is (= (n/shift (n/to-nines -1M) 1M)
         (n/to-nines -1M)))

  (is (= (n/shift (n/to-nines 1M) 0.1M)
         (n/to-nines 0.1M)))

  (is (= (n/shift (n/to-nines -1M) 0.1M)
         (n/to-nines -0.1M)))

  (is (= (n/shift (n/to-nines 1M) 0.0000001M)
         (n/to-nines 0.0000001M)))

  (is (= (n/shift (n/to-nines -1M) 0.0000001M)
         (n/to-nines -0.0000001M)))

  (is (= (n/shift (n/to-nines 0.01M) 10M)
         (n/to-nines 0.1M)))

  (is (= (n/shift (n/to-nines -0.01M) 10M)
         (n/to-nines -0.1M)))

  (is (= (n/shift 99.98999999999999M 10M)
         99.89999999999999M))
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