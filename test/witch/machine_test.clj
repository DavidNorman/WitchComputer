(ns witch.machine-test
  (:require [clojure.test :refer :all]
            [witch.machine :as m])
  (:import (clojure.lang ExceptionInfo)))

; Initial state

(deftest initial-state

  (is (= (:registers m/initial-machine-state) (into [] (repeat 90 0))))
  (is (= (:shift-value m/initial-machine-state) 1))
  (is (= (:finished m/initial-machine-state) false))
  )

; Writing to stores / accumultor

(deftest write-to-stores

  ; typical write
  (is (= (-> m/initial-machine-state
             (m/write-address 10 1M)
             :registers
             (get 0))
         1M))

  ; another address
  (is (= (-> m/initial-machine-state
             (m/write-address 20 7.123456M)
             :registers
             (get 10))
         7.123456M))

  ; negative number
  (is (= (-> m/initial-machine-state
             (m/write-address 21 -5M)
             :registers
             (get 11))
         -5M))

  ; truncated number
  (is (= (-> m/initial-machine-state
             (m/write-address 21 0.00000001)
             :registers
             (get 11))
         0M))

  ; register out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (m/write-address 100 0))))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (m/write-address 10 11M))))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (m/write-address 10 -10M))))
  )

(deftest write-to-accumulator

  ; typical write
  (is (= (-> m/initial-machine-state
             (m/write-address 9 1M)
             :accumulator)
         1M))

  ; truncated
  (is (= (-> m/initial-machine-state
             (m/write-address 9 1.111111111111111M)
             :accumulator)
         1.11111111111111M))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (m/write-address 10 10M))))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (m/write-address 10 -10M))))
  )

; Reading from stores / special addresses

(deftest read-from-stores

  ; typical read of source value
  (is (= (-> m/initial-machine-state
             (assoc-in [:registers 0] 1.01M)
             (m/read-src-address 10)
             (first))
         1.01M))

  ; typical read of desination value
  (is (= (-> m/initial-machine-state
             (assoc-in [:registers 11] -1.01M)
             (m/read-dst-address 21)
             (first))
         -1.01M))
  )

(deftest read-from-accumulator

  ; typical read of source value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator 1.01M)
             (m/read-src-address 9)
             (first))
         1.01M))

  ; typical read of desination value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator -1.01M)
             (m/read-dst-address 9)
             (first))
         -1.01M))

  ; typical read of source value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator 0.12345678912345M)
             (m/read-src-address 8)
             (first))
         8.912345M))

  ; typical read of source value (top 8 digits, negative)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator -0.12345678912345M)
             (m/read-src-address 8)
             (first))
         -8.912345M))

  ; typical read of desination value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator -0.12345678912345M)
             (m/read-dst-address 8)
             (first))
         0M))

  )


; Operations

(deftest search-tape

  ; Find the next block1 marker and advance one entry past it
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[1M 2M 3M :block1 4M :block1 5M :block2 6M]])
             (m/search-tape 1 :block1)
             (get-in [:tapes 0]))
         [:block1 4M :block1 5M :block2 6M 1M 2M 3M]))
  )

(deftest read-from-tape

  ; Returns the correct value
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[1M 2M 3M :block1 4M :block1 5M :block2 6M]])
             (m/input-tape 1)
             (first))
         1M))

  ; Moves the tape forward
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[1M 2M 3M :block1 4M :block1 5M :block2 6M]])
             (m/input-tape 1)
             (second)
             (get-in [:tapes 0]))
         [2M 3M :block1 4M :block1 5M :block2 6M 1M]))

  ; Can't read non-existing tape
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :tapes [[1M 2M 3M :block1 4M :block1 5M :block2 6M]])
                   (m/input-tape 2))))

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
