(ns witch.nines-test
  (:require [clojure.test :refer :all]
            [witch.test-helper :as h]
            [witch.nines :as n]))

(deftest to-nines
  (is (= (h/value-and-scale (n/to-nines 1.0000000M))
         [1.00000000M 7]))

  (is (= (h/value-and-scale (n/to-nines 0.0100000M))
         [0.01000000M 7]))

  (is (= (h/value-and-scale (n/to-nines -1.0000000M))
         [98.9999999M 7]))

  (is (= (h/value-and-scale (n/to-nines -0.0010000M))
         [99.9989999M 7]))

  (is (= (h/value-and-scale (n/to-nines -0.0000001M))
         [99.9999998M 7]))
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
  (is (= (h/value-and-scale (n/adjust-places 0.00000001M 4))
         [0.0000M 4]))

  (is (= (h/value-and-scale (n/adjust-places 0.22222222M 5))
         [0.22222M 5]))

  (is (= (h/value-and-scale (n/adjust-places 0.99999999M 5))
         [0.99999M 5]))

  (is (= (h/value-and-scale (n/adjust-places 99.22222222M 5))
         [99.22222M 5]))

  (is (= (h/value-and-scale (n/adjust-places 9999.222222222222M 5))
         [99.22222M 5]))

  (is (= (h/value-and-scale (n/adjust-places 9999.999999999999M 5))
         [99.99999M 5]))
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
  (is (= (h/value-and-scale (n/negate 1.111M))
         [98.888M 3]))

  (is (= (h/value-and-scale (n/negate 1.1111111M))
         [98.8888888M 7]))

  (is (= (h/value-and-scale (n/negate 2.22222222222222M))
         [97.77777777777777M 14]))
  )

(deftest carry-over
  (is (= (h/value-and-scale (n/carry-over 100.000000M 7))
         [00.0000001M 7]))

  (is (= (h/value-and-scale (n/carry-over 100.0000000000000M 14))
         [00.00000000000001M 14]))

  (is (= (h/value-and-scale (n/carry-over 23400.0000000000000M 14))
         [00.00000000000234M 14]))

  (is (= (h/value-and-scale (n/carry-over 199.99999999999998M 14))
         [99.99999999999999M 14]))
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

(deftest sign-extend-left
  (is (= (h/value-and-scale (n/sign-extend-left 0.0000000M))
         [0.0000000M 7]))

  (is (= (h/value-and-scale (n/sign-extend-left 90.0000000M))
         [999999990.0000000M 7]))

  (is (= (h/value-and-scale (n/sign-extend-left 99.9999999M))
         [999999999.9999999M 7]))
  )

(deftest sign-extend-right
  (is (= (h/value-and-scale (n/sign-extend-right 0.0000000M))
         [0.000000000000000M 14]))

  (is (= (h/value-and-scale (n/sign-extend-right 90.0000000M))
         [90.00000009999999M 14]))

  (is (= (h/value-and-scale (n/sign-extend-right 99.9999999M))
         [99.99999999999999M 14]))

  (is (= (h/value-and-scale (n/sign-extend-right 0.00000000000001M))
         [0.00000000000001M 14]))

  (is (= (h/value-and-scale (n/sign-extend-right 99.12345678901234M))
         [99.12345678901234M 14]))
  )

(deftest sign
  (is (= (h/value-and-scale (n/sign 99.99999999999998M))
         [9M 0]))

  (is (= (h/value-and-scale (n/sign 00.00000000000000M))
         [0M 0]))

  (is (= (h/value-and-scale (n/sign 09.9999999M))
         [0M 0]))

  (is (= (h/value-and-scale (n/sign 91.1111111M))
         [9M 0]))

  )
(deftest positive
  (is (= (n/positive? 0.0000000M))
      true)

  (is (= (n/positive? 1.0000000M))
      true)

  (is (= (n/positive? 0.0000001M))
      true)

  (is (= (n/positive? 90.0000001M))
      false)

  (is (= (n/positive? 99.0000001M))
      false)

  (is (= (n/positive? 90.9999999M))
      false)
  )

(deftest negative
  (is (= (n/negative? 0.0000000M))
      false)

  (is (= (n/negative? 1.0000000M))
      false)

  (is (= (n/negative? 0.0000001M))
      false)

  (is (= (n/negative? 90.0000001M))
      true)

  (is (= (n/negative? 99.0000001M))
      true)

  (is (= (n/negative? 90.9999999M))
      true)
  )

(deftest units-digit

  (is (= (n/units-digit 0.0000001M))
      0M)

  (is (= (n/units-digit 1.0000001M))
      1M)

  (is (= (n/units-digit 91.9999999M))
      1M)

  (is (= (n/units-digit 99.9999999M))
      9M)

  (is (= (n/units-digit 9.9999999M))
      9M)
  )

(deftest get-digit

  (is (= (h/value-and-scale (n/get-digit 1.2345678M 0)))
      [1M 0])

  (is (= (h/value-and-scale (n/get-digit 1.2345678M 1)))
      [2M 0])

  (is (= (h/value-and-scale (n/get-digit 1.2345678M 7)))
      [8M 0])

  (is (= (h/value-and-scale (n/get-digit 91.2345678M 0)))
      [1M 0])

  (is (= (h/value-and-scale (n/get-digit 91.2345678M 7)))
      [8M 0])
  )