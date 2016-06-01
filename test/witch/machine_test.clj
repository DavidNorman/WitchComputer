(ns witch.machine-test
  (:require [clojure.test :refer :all]
            [witch.test-helper :as h]
            [witch.machine :as m])
  (:import (clojure.lang ExceptionInfo)))

; Initial state

(deftest initial-state

  (is (= (:stores m/initial-machine-state) (into [] (repeat 90 0.0000000M))))
  (is (= (:transfer-shift m/initial-machine-state) 0M))
  (is (= (:transfer-complement m/initial-machine-state) :false))
  (is (= (:finished m/initial-machine-state) false))
  )

; Writing to stores / accumultor

(deftest write-to-stores

  ; typical write
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 1.00000000000000M)
             (assoc-in [:stores 0] 0.0000000M)
             (m/write-address 10)
             :stores
             (get 0))
         1.0000000M))

  ; another address
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 7.1234560000000M)
             (assoc-in [:stores 0] 0.0000000M)
             (m/write-address 20)
             :stores
             (get 10))
         7.123456M))

  ; negative number
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 95.99999999999999M)
             (assoc-in [:stores 0] 0.00000000M)
             (m/write-address 21)
             :stores
             (get 11))
         95.9999999M))

  ; summation
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 1.00000000000000M)
             (assoc-in [:stores 0] 1.0000000M)
             (m/write-address 10)
             :stores
             (get 0))
         2.0000000M))

  ; summation of negative number
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 97.99999999999999M)
             (assoc-in [:stores 0] 1.0000000M)
             (m/write-address 10)
             :stores
             (get 0))
         98.9999999M))

  ; truncated number
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 0.000000010000000M)
             (assoc-in [:stores 0] 0.0M)
             (m/write-address 21)
             :stores
             (get 11))
         0.0000000M))

  ; register out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :transfer-output 0.0000000M)
                   (m/write-address 100))))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :transfer-output 10.0000000M)
                   (m/write-address 10))))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :transfer-output 89.9999999M)
                   (m/write-address 10))))
  )

(deftest write-to-accumulator

  ; typical write
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 1.00000000000000M)
             (assoc :accumulator 0.00000000000000M)
             (m/write-address 9)
             :accumulator)
         1.00000000000000M))

  ; truncated
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 1.111111111111111M)
             (assoc :accumulator 0.00000000000000M)
             (m/write-address 9)
             :accumulator)
         1.11111111111111M))

  ; summation
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 1.00000000000000M)
             (assoc :accumulator 1.00000000000000M)
             (m/write-address 9)
             :accumulator)
         2.00000000000000M))

  ; summation of negative number
  (is (= (-> m/initial-machine-state
             (assoc :transfer-output 97.99999999999999M)
             (assoc :accumulator 1.00000000000000M)
             (m/write-address 9)
             :accumulator)
         98.99999999999999M))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :transfer-output 10.00000000000000M)
                   (m/write-address 9))))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :transfer-output 89.99999999999999M)
                   (m/write-address 9))))
  )

; Reading from stores / special addresses

(deftest read-from-stores

  ; typical read of source value
  (is (= (-> m/initial-machine-state
             (assoc-in [:stores 0] 1.0100000M)
             (m/read-sending-address 10)
             :sending-value)
         1.0100000M))
  )

(deftest read-from-accumulator

  ; typical read of source value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator 1.0100000M)
             (m/read-sending-address 9)
             :sending-value
             h/value-and-scale)
         [1.0100000M 7]))

  ; typical read of source value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator 0.12345678912345M)
             (m/read-sending-address 8)
             :sending-value
             h/value-and-scale)
         [8.912345M 7]))

  ; typical read of source value (top 8 digits, negative)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator 99.12345678901234M)
             (m/read-sending-address 8)
             :sending-value
             h/value-and-scale)
         [98.9012340M 7]))
  )

(deftest clear

  ; not clearing source value
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 99.8999999M)
             (assoc :sending-clear true)
             (#'witch.machine/clear)
             h/value-and-scale)
         [0.0000000M 7]))

  ; clearing source value
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 99.8999999M)
             (assoc :sending-clear true)
             (#'witch.machine/clear)
             h/value-and-scale)
         [0.0000000M 7]))

  ; accumulator not clearing
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 1.00000000000000M)
             (assoc :sending-clear false)
             (#'witch.machine/clear)
             h/value-and-scale)
         [1.00000000000000M 14]))

  ; accumulator clearing
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 1.00000000000000M)
             (assoc :sending-clear true)
             (#'witch.machine/clear)
             h/value-and-scale)
         [0.00000000000000M 14]))
  )

; Operations

(deftest search-tape

  ; Find the next block1 marker
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[[nil 1M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]])
             (m/search-tape 1 :block1)
             (get-in [:tapes 0]))
         [[:block1 4M] [:block1 5M] [:block2 6M] [nil 1M] [nil 2M] [nil 3M]]))
  )

(deftest read-from-tape

  ; Returns the correct value
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[[nil 1M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]])
             (m/input-tape 1)
             :sending-value)
         1M))

  ; Moves the tape forward
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[[nil 1M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]])
             (m/input-tape 1)
             (get-in [:tapes 0]))
         [[nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M] [nil 1M]]))

  ; Can't read non-existing tape
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :tapes [[[nil 1M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]])
                   (m/input-tape 2))))

  )

(deftest transfer

  ; unchanged
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.2000000M)
             (assoc :transfer-shift 0)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [0.20000000000000M 14]))

  ; shift x10
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.2000000M)
             (assoc :transfer-shift 1)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [2.00000000000000M 14]))

  ; shift x10
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 98.2999999M)
             (assoc :transfer-shift 1)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [92.99999999999990M 14]))

  ; shift x0.1
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.2000000M)
             (assoc :transfer-shift -1)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [0.02000000000000M 14]))

  ; shift x0.0000001
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 2.0000000M)
             (assoc :transfer-shift -7)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [0.00000020000000M 14]))

  ; shift x0.0000001
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.0000002M)
             (assoc :transfer-shift -7)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [0.00000000000002M 14]))

  ; complement
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.2000000M)
             (assoc :transfer-complement :true)
             (assoc :transfer-shift 0)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [99.79999999999999M 14]))

  ; complement and shift x10
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.2000000M)
             (assoc :transfer-complement :true)
             (assoc :transfer-shift 1)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [97.99999999999999M 14]))

  ; complement and shift X0.1
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.2000000M)
             (assoc :transfer-complement :true)
             (assoc :transfer-shift -1)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [99.97999999999999M 14]))

  ; complement and shift x0.0000001
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.0000001M)
             (assoc :transfer-complement :true)
             (assoc :transfer-shift -7)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [99.99999999999998M 14]))

  ; source conditional complement
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.2000000M)
             (assoc :transfer-complement :sending)
             (assoc :transfer-shift 0)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [0.20000000000000M 14]))

  (is (= (-> m/initial-machine-state
             (assoc :sending-value 99.7999999M)
             (assoc :transfer-complement :sending)
             (assoc :transfer-shift 0)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [0.20000000000000M 14]))

  ; source conditional complement and shift x0.0000001
  (is (= (-> m/initial-machine-state
             (assoc :sending-value 0.0000001M)
             (assoc :transfer-complement :sending)
             (assoc :transfer-shift -7)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [0.00000000000001M 14]))

  (is (= (-> m/initial-machine-state
             (assoc :sending-value 99.9999998M)
             (assoc :transfer-complement :sending)
             (assoc :transfer-shift -7)
             (m/transfer)
             :transfer-output
             (h/value-and-scale)
             )
         [0.00000000000001M 14]))

  )

(deftest advance-pc

  ; When PC is a tape - it isn't incremented
  (is (= (-> m/initial-machine-state
             (assoc :pc 1)
             (m/advance-pc)
             :pc)
         1))

  ; When PC is a store - it is incremented
  (is (= (-> m/initial-machine-state
             (assoc :pc 10)
             (m/advance-pc)
             :pc)
         11))
  )
