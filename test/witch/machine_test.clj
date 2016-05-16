(ns witch.machine-test
  (:require [clojure.test :refer :all]
            [witch.machine :as m])
  (:import (clojure.lang ExceptionInfo)))

; Initial state

(deftest initial-state

  (is (= (:registers m/initial-machine-state) (into [] (repeat 90 0M))))
  (is (= (:shift-value m/initial-machine-state) 1M))
  (is (= (:finished m/initial-machine-state) false))
  )

; Writing to stores / accumultor

(deftest write-to-stores

  ; typical write
  (is (= (-> m/initial-machine-state
             (assoc :alu-result 1M)
             (m/write-address 10)
             :registers
             (get 0))
         1M))

  ; another address
  (is (= (-> m/initial-machine-state
             (assoc :alu-result 7.123456M)
             (m/write-address 20)
             :registers
             (get 10))
         7.123456M))

  ; negative number
  (is (= (-> m/initial-machine-state
             (assoc :alu-result -5M)
             (m/write-address 21)
             :registers
             (get 11))
         -5M))

  ; truncated number
  (is (= (-> m/initial-machine-state
             (assoc :alu-result 0.00000001M)
             (m/write-address 21)
             :registers
             (get 11))
         0M))

  ; register out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :alu-result 0M)
                   (m/write-address 100))))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :alu-result 11M)
                   (m/write-address 10))))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :alu-result -10M)
                   (m/write-address 10))))
  )

(deftest write-to-accumulator

  ; typical write
  (is (= (-> m/initial-machine-state
             (assoc :alu-result 1M)
             (m/write-address 9)
             :accumulator)
         1M))

  ; truncated
  (is (= (-> m/initial-machine-state
             (assoc :alu-result 1.111111111111111M)
             (m/write-address 9)
             :accumulator)
         1.11111111111111M))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :alu-result 10M)
                   (m/write-address 10))))

  ; number out of range
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :alu-result -10M)
                   (m/write-address 10))))
  )

; Reading from stores / special addresses

(deftest read-from-stores

  ; typical read of source value
  (is (= (-> m/initial-machine-state
             (assoc-in [:registers 0] 1.01M)
             (m/read-src-address 10)
             :alu-src)
         1.01M))

  ; typical read of desination value
  (is (= (-> m/initial-machine-state
             (assoc-in [:registers 11] -1.01M)
             (m/read-dst-address 21)
             :alu-dst)
         -1.01M))
  )

(deftest read-from-accumulator

  ; typical read of source value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator 1.01M)
             (m/read-src-address 9)
             :alu-src)
         1.01M))

  ; typical read of desination value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator -1.01M)
             (m/read-dst-address 9)
             :alu-dst)
         -1.01M))

  ; typical read of source value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator 0.12345678912345M)
             (m/read-src-address 8)
             :alu-src)
         8.912345M))

  ; typical read of source value (top 8 digits, negative)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator -0.12345678912345M)
             (m/read-src-address 8)
             :alu-src)
         -8.912345M))

  ; typical read of desination value (top 8 digits)
  (is (= (-> m/initial-machine-state
             (assoc :accumulator -0.12345678912345M)
             (m/read-dst-address 8)
             :alu-dst)
         0M))

  )


; Operations

(deftest search-tape

  ; Find the next block1 marker and advance one entry past it
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
             (m/input-tape :alu-src 1)
             :alu-src)
         1M))

  ; Moves the tape forward
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[[nil 1M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]])
             (m/input-tape :alu-src 1)
             (get-in [:tapes 0]))
         [[nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M] [nil 1M]]))

  ; Can't read non-existing tape
  (is (thrown? ExceptionInfo
               (-> m/initial-machine-state
                   (assoc :tapes [[[nil 1M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]])
                   (m/input-tape :alu-src 2))))

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
