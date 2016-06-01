(ns witch.decode-test
  (:require [clojure.test :refer :all]
            [witch.machine :as m]
            [witch.decode :as d]
            [witch.nines :as n])
  (:import (clojure.lang ExceptionInfo)))

(defn get-registers
  "Utility for pulling a collection of registers"
  [machine-state regs]
  (map #(get (:stores machine-state) %) regs))

; Instructions

(deftest invalid-instruction

         ;; Main opcode set
         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[[nil 8.0000M]]])
                        (assoc :pc 1)
                        (d/step))))

         ;; Control opcode
         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[[nil 0.6000M]]])
                        (assoc :pc 1)
                        (d/step))))

         )

(deftest add

         ;; Successful op
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:stores 0] (n/to-nines 1.234M))
                  (assoc-in [:stores 10] (n/to-nines 0.001M))
                  (assoc :tapes [[[nil 1.1020M]]])
                  (assoc :pc 1)
                  (d/step)
                  (get-registers [0 10]))
                [1.234M 1.235M]))


         ;; Sum two negative numbers
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:stores 0] 99.8999999M)
                  (assoc-in [:stores 10] 98.9999999M)
                  (assoc :tapes [[[nil 1.1020M]]])
                  (assoc :pc 1)
                  (d/step)
                  (get-registers [0 10]))
                [99.8999999M 98.8999999M]))

         ;; Sum through zero + -> -
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:stores 0] 0.1000000M)
                  (assoc-in [:stores 10] 98.9999999M)
                  (assoc :tapes [[[nil 1.1020M]]])
                  (assoc :pc 1)
                  (d/step)
                  (get-registers [10]))
                [99.0999999M]))

         ;; Sum through zero - -> +
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:stores 0] 98.9999999M)
                  (assoc-in [:stores 10] 2.0000000M)
                  (assoc :tapes [[[nil 1.1020M]]])
                  (assoc :pc 1)
                  (d/step)
                  (get-registers [10]))
                [1.0000000M]))

         ; Into accumulator
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:stores 0] (n/to-nines 1.234M))
                  (assoc :accumulator 0.0000001111M)
                  (assoc :tapes [[[nil 1.1009M]]])
                  (assoc :pc 1)
                  (d/step)
                  :accumulator)
                1.2340001111M))

         ; From accumulator
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:stores 10] (n/to-nines 1.3333333M))
                  (assoc :accumulator 0.0000001111M)
                  (assoc :tapes [[[nil 1.0920M]]])
                  (assoc :pc 1)
                  (d/step)
                  (get-registers [10]))
                [1.3333334M]))

         ;; Overflow and underflow
         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc-in [:stores 0] (n/to-nines 8.0M))
                        (assoc-in [:stores 10] (n/to-nines 2.0M))
                        (assoc :tapes [[[nil 1.1020M]]])
                        (assoc :pc 1)
                        (d/step))))

         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc-in [:stores 0] (n/to-nines -8.0M))
                        (assoc-in [:stores 10] (n/to-nines -3.0M))
                        (assoc :tapes [[[nil 1.1020M]]])
                        (assoc :pc 1)
                        (d/step))))

         ;; Invalid stores
         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[[nil 1.1011M]]])
                        (assoc :pc 1)
                        (d/step))))

         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[[nil 1.2029M]]])
                        (assoc :pc 1)
                        (d/step))))

         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[[nil 1.0000M]]])
                        (assoc :pc 1)
                        (d/step))))

         ;; With shift
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:stores 0] (n/to-nines 0.1234M))
                  (assoc-in [:stores 10] (n/to-nines 0.001M))
                  (assoc :tapes [[[nil 0.8100] [nil 1.1020M]]])
                  (assoc :pc 1)
                  (d/step)
                  (d/step)
                  (get-registers [0 10]))
                [0.1234M 1.235M]))
         )

(deftest add-clear

  ;; Successful op
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc-in [:stores 10] (n/to-nines 0.001M))
           (assoc :tapes [[[nil 2.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [0M 1.235M]))

  ; Into accumulator
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[[nil 2.1009M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         1.2340001111M))

  ; From accumulator
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 10] (n/to-nines 1.3333333M))
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[[nil 2.0920M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [10]))
         [1.3333334M]))

  ;; Clear accumulator
  (is (= (->
           m/initial-machine-state
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[[nil 2.0900M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0M))

  ;; Overflow and underflow
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines 8.0M))
                 (assoc-in [:stores 10] (n/to-nines 2.0M))
                 (assoc :tapes [[[nil 2.1020M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines -8.0M))
                 (assoc-in [:stores 10] (n/to-nines -3.0M))
                 (assoc :tapes [[[nil 2.1020M]]])
                 (assoc :pc 1)
                 (d/step))))

  ;; Invalid stores
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 2.1011M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 2.2029M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 2.0000M]]])
                 (assoc :pc 1)
                 (d/step))))
  )

(deftest subtract
  ;; Successful op (+ve - +ve)
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 1.2340000M)
           (assoc-in [:stores 10] 0.0010000M)
           (assoc :tapes [[[nil 3.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [1.2340000M 98.7669999M]))

  ;; Successful op (-ve - +ve)
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 1.2340000M)
           (assoc-in [:stores 10] 99.8999999M)
           (assoc :tapes [[[nil 3.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [1.2340000M 98.6659999M]))

  ;; Successful op (-ve - +ve)
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 98.9999999M)
           (assoc-in [:stores 10] 1.0000000M)
           (assoc :tapes [[[nil 3.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [98.9999999M 2.0000000M]))

  ;; Successful op (-ve - -ve)
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 99.8999999M)
           (assoc-in [:stores 10] 98.9999999M)
           (assoc :tapes [[[nil 3.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [99.8999999M 99.0999999M]))

  ; Into accumulator (+ve - +ve)
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 1.0000000M)
           (assoc :accumulator 02.00000011111111M)
           (assoc :tapes [[[nil 3.1009M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         1.00000011111111M))

  ; Into accumulator (+ve - -ve) ; TODO is this right (or 3.3.23400021111110M)?
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 98.7659999M)
           (assoc :accumulator 02.00000011111111M)
           (assoc :tapes [[[nil 3.1009M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         3.23400011111111M))

  ; From accumulator (lower digits are dropped before subtraction)
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 10] 1.0000002M)
           (assoc :accumulator 00.00000011100000M)
           (assoc :tapes [[[nil 3.0920M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [10]))
         [(n/to-nines 1.0000001M)]))

  ;; Overflow and underflow
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines 8.0M))
                 (assoc-in [:stores 10] (n/to-nines -2.0M))
                 (assoc :tapes [[[nil 3.1020M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines -8.0M))
                 (assoc-in [:stores 10] (n/to-nines 3.0M))
                 (assoc :tapes [[[nil 3.1020M]]])
                 (assoc :pc 1)
                 (d/step))))

  ;; Invalid stores
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 3.1011M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 3.2029M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 3.0000M]]])
                 (assoc :pc 1)
                 (d/step))))

  ;; With shift
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 0.1234M))
           (assoc-in [:stores 10] (n/to-nines 0.001M))
           (assoc :tapes [[[nil 0.8100] [nil 3.1020M]]])
           (assoc :pc 1)
           (d/step)
           (d/step)
           (get-registers [0 10]))
         [(n/to-nines 0.1234M) (n/to-nines -1.233M)]))
  )

(deftest subtract-clear
  ;; Successful op
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc-in [:stores 10] (n/to-nines 0.001M))
           (assoc :tapes [[[nil 4.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [(n/to-nines 0M) (n/to-nines -1.233M)]))

  ;; Clear accumulator
  (is (= (->
           m/initial-machine-state
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[[nil 4.0900M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0M))

  ;; Overflow and underflow
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines 8.0M))
                 (assoc-in [:stores 10] (n/to-nines -2.0M))
                 (assoc :tapes [[[nil 4.1020M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines -8.0M))
                 (assoc-in [:stores 10] (n/to-nines 3.0M))
                 (assoc :tapes [[[nil 4.1020M]]])
                 (assoc :pc 1)
                 (d/step))))

  ;; Invalid stores
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 4.1011M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 4.2029M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 4.0000M]]])
                 (assoc :pc 1)
                 (d/step))))
  )

(deftest multiply

  ; Successful multiply
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 1.2340000M)
           (assoc-in [:stores 10] 0.0010000M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0.00123400000000M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 0.0000001M)
           (assoc-in [:stores 10] 0.0000001M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0.00000000000001M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 1.2340000M)
           (assoc-in [:stores 10] 98.9999999M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         98.76599999999999M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 99.9999994M)
           (assoc-in [:stores 10] 0.0050000M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         99.99999999749999M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 99.9999998M)
           (assoc-in [:stores 10] 0.0000001M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         99.99999999999998M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 0.0000001M)
           (assoc-in [:stores 10] 99.9999998M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         99.99999999999998M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 99.9999998M)
           (assoc-in [:stores 10] 99.9999998M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0.00000000000001M))

  ; Receiving address is cleared, sending address isn't cleared
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 99.9999998M)
           (assoc-in [:stores 10] 1.0000000M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [99.9999998M 0.0000000M]))

  ; Receiving address is cleared, sending address isn't cleared
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 99.9999998M)
           (assoc-in [:stores 10] 99.9999998M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [99.9999998M 0.0000000M]))

  ; A non-empty accumulator has the value added
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 1.2340000M)
           (assoc-in [:stores 10] 0.0010000M)
           (assoc :accumulator 1.50000000000000M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         1.50123400000000M))

  ; Overflow
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] 95.9999999M)
                 (assoc-in [:stores 10] 96.9999999M)
                 (assoc :tapes [[[nil 5.1020M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] 4.0000000M)
                 (assoc-in [:stores 10] 3.0000000M)
                 (assoc :tapes [[[nil 5.1020M]]])
                 (assoc :pc 1)
                 (d/step))))

  ; Addresses must not be in group 00-09
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 5.0020M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 5.1001M]]])
                 (assoc :pc 1)
                 (d/step))))
  )

#_(deftest divide

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 2.0M)
           (assoc :accumulator 6.0M)
           (assoc :tapes [[[nil 6.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [2.0M 3.0M]))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 2.0M)
           (assoc :accumulator 7.0M)
           (assoc :tapes [[[nil 6.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [2.0M 3.5M]))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] -2.0M)
           (assoc :accumulator 7.0M)
           (assoc :tapes [[[nil 6.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [-2.0M -3.5M]))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 2.0M)
           (assoc :accumulator -7.0M)
           (assoc :tapes [[[nil 6.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [2.0M -3.5M]))

  ;TODO remainder in accumulator
  ;TODO positive zero dividend is an error
  )

(deftest transfer-modulus

  ; Transfer positive number
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 1.2340000M)
           (assoc-in [:stores 10] 0.0010000M)
           (assoc :tapes [[[nil 7.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [1.2340000M 1.2350000M]))

  ; Transfer negative number
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] 98.7659999M)
           (assoc-in [:stores 10] 0.0010000M)
           (assoc :tapes [[[nil 7.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [98.7659999M 1.2350000M]))
  )

(deftest set-print-layout
  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.7100M]]])
           (assoc :pc 1)
           (d/step)
           :printing-layout)
         1))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.7200M]]])
           (assoc :pc 1)
           (d/step)
           :printing-layout)
         2))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.7900M]]])
           (assoc :pc 1)
           (d/step)
           :printing-layout)
         9))
  )

(deftest set-shift
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 0.8000M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8100M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         1M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8200M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         0M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8300M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         -1M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8400M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         -2M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8500M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         -3M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8600M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         -4M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8700M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         -5M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8800M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         -6M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8900M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         -7M))
  )

(deftest search-tape

  ; Find the next block1 marker in executing tape
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[[nil 0.3101M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]])
             (assoc :pc 1)
             (d/step)
             (get-in [:tapes 0]))
         [[:block1 4M] [:block1 5M] [:block2 6M] [nil 0.3101M] [nil 2M] [nil 3M]]))

  ; Find the next block1 marker in another tape
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[[nil 0.3203M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]
                            [[nil 1M] [nil 2M] [:block3 3M]]])
             (assoc :pc 1)
             (d/step)
             :tapes)
         [[[nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M] [nil 0.3203M]]
          [[:block3 3M] [nil 1M] [nil 2M]]]))

  ; Conditional positive
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[[nil 0.5203M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]
                            [[nil 1M] [nil 2M] [:block3 3M]]])
             (assoc :sign-test true)
             (assoc :pc 1)
             (d/step)
             :tapes)
         [[[nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M] [nil 0.5203M]]
          [[:block3 3M] [nil 1M] [nil 2M]]]))

  ; Conditional negative
  (is (= (-> m/initial-machine-state
             (assoc :tapes [[[nil 0.5203M] [nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M]]
                            [[nil 1M] [nil 2M] [:block3 3M]]])
             (assoc :sign-test false)
             (assoc :pc 1)
             (d/step)
             :tapes)
         [[[nil 2M] [nil 3M] [:block1 4M] [:block1 5M] [:block2 6M] [nil 0.5203M]]
          [[nil 1M] [nil 2M] [:block3 3M]]]))
  )





(deftest transfer-control

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.0000M]]])
           (assoc :pc 1)
           (d/step)
           :finished)
         false))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.0000M]]])
           (assoc :pc 1)
           (d/step)
           :finished)
         false))
  )

(deftest sign-examination

  ; Positive true
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 2.1M))
           (assoc :tapes [[[nil 0.1110M]]])
           (assoc :pc 1)
           (d/step)
           :sign-test)
         true))


  ; Negative true
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines -2.1M))
           (assoc :tapes [[[nil 0.1210M]]])
           (assoc :pc 1)
           (d/step)
           :sign-test)
         true))

  ; Positive false
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines -2.1M))
           (assoc :tapes [[[nil 0.1110M]]])
           (assoc :pc 1)
           (d/step)
           :sign-test)
         false))


  ; Negative false
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 2.1M))
           (assoc :tapes [[[nil 0.1210M]]])
           (assoc :pc 1)
           (d/step)
           :sign-test)
         false))

  ; Bad opcode
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines 2.1M))
                 (assoc :tapes [[[nil 0.1010M]]])
                 (assoc :pc 1)
                 (d/step)
                 :sign-test)
               false))

  ; Bad opcode
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines 2.1M))
                 (assoc :tapes [[[nil 0.1310M]]])
                 (assoc :pc 1)
                 (d/step)
                 :sign-test)
               false))
  )

(deftest signal

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.0000M]]])
           (assoc :pc 1)
           (d/step)
           :finished)
         false))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.0100M]]])
           (assoc :pc 1)
           (d/step)
           :finished)
         true))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.0200M]]])
           (assoc :pc 1)
           (d/step)
           :finished)
         true))


  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[[nil 0.0300M]]])
                 (assoc :pc 1)
                 (d/step))))
  )
