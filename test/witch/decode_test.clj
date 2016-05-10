(ns witch.decode-test
  (:require [clojure.test :refer :all]
            [witch.machine :as m]
            [witch.decode :as d])
  (:import (clojure.lang ExceptionInfo)))

(defn get-registers
  "Utility for pulling a collection of registers"
  [machine-state regs]
  (map #(get (:registers machine-state) %) regs))

; Instructions

(deftest invalid-instruction

         ;; Main opcode set
         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[8.0000M]])
                        (assoc :pc 1)
                        (d/step))))

         ;; Control opcode
         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[0.6000M]])
                        (assoc :pc 1)
                        (d/step))))

         )

(deftest add

         ;; Successful op
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:registers 0] 1.234M)
                  (assoc-in [:registers 10] 0.001M)
                  (assoc :tapes [[1.1020M]])
                  (assoc :pc 1)
                  (d/step)
                  (get-registers [0 10]))
                [1.234M 1.235M]))

         ; Into accumulator
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:registers 0] 1.234M)
                  (assoc :accumulator 0.0000001111M)
                  (assoc :tapes [[1.1009M]])
                  (assoc :pc 1)
                  (d/step)
                  :accumulator)
                1.2340001111M))

         ; From accumulator
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:registers 10] 1.3333333M)
                  (assoc :accumulator 0.0000001111M)
                  (assoc :tapes [[1.0920M]])
                  (assoc :pc 1)
                  (d/step)
                  (get-registers [10]))
                [1.3333334M]))
         ;; Overflow and underflow
         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc-in [:registers 0] 8.0M)
                        (assoc-in [:registers 10] 2.0M)
                        (assoc :tapes [[1.1020M]])
                        (assoc :pc 1)
                        (d/step))))

         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc-in [:registers 0] -8.0M)
                        (assoc-in [:registers 10] -3.0M)
                        (assoc :tapes [[1.1020M]])
                        (assoc :pc 1)
                        (d/step))))

         ;; Invalid stores
         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[1.1011M]])
                        (assoc :pc 1)
                        (d/step))))

         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[1.2029M]])
                        (assoc :pc 1)
                        (d/step))))

         (is (thrown? ExceptionInfo
                      (->
                        m/initial-machine-state
                        (assoc :tapes [[1.0000M]])
                        (assoc :pc 1)
                        (d/step))))

         ;; With shift
         (is (= (->
                  m/initial-machine-state
                  (assoc-in [:registers 0] 0.1234M)
                  (assoc-in [:registers 10] 0.001M)
                  (assoc :tapes [[0.8000 1.1020M]])
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
           (assoc-in [:registers 0] 1.234M)
           (assoc-in [:registers 10] 0.001M)
           (assoc :tapes [[2.1020M]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [0M 1.235M]))

  ; Into accumulator
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 1.234M)
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[2.1009M]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         1.2340001111M))

  ; From accumulator
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 10] 1.3333333M)
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[2.0920M]])
           (assoc :pc 1)
           (d/step)
           (get-registers [10]))
         [1.3333334M]))

  ;; Clear accumulator
  (is (= (->
           m/initial-machine-state
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[2.0900M]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0M))

  ;; Overflow and underflow
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:registers 0] 8.0M)
                 (assoc-in [:registers 10] 2.0M)
                 (assoc :tapes [[2.1020M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:registers 0] -8.0M)
                 (assoc-in [:registers 10] -3.0M)
                 (assoc :tapes [[2.1020M]])
                 (assoc :pc 1)
                 (d/step))))

  ;; Invalid stores
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[2.1011M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[2.2029M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[2.0000M]])
                 (assoc :pc 1)
                 (d/step))))
  )

(deftest subtract
  ;; Successful op
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 1.234M)
           (assoc-in [:registers 10] 0.001M)
           (assoc :tapes [[3.1020M]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [1.234M -1.233M]))

  ; Into accumulator
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 1.234M)
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[3.1009M]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         -1.2339998889M))

  ; From accumulator
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 10] 1.3333333M)
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[3.0920M]])
           (assoc :pc 1)
           (d/step)
           (get-registers [10]))
         [1.3333331M]))  ; <-- is this right?

  ;; Overflow and underflow
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:registers 0] 8.0M)
                 (assoc-in [:registers 10] -2.0M)
                 (assoc :tapes [[3.1020M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:registers 0] -8.0M)
                 (assoc-in [:registers 10] 3.0M)
                 (assoc :tapes [[3.1020M]])
                 (assoc :pc 1)
                 (d/step))))

  ;; Invalid stores
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[3.1011M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[3.2029M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[3.0000M]])
                 (assoc :pc 1)
                 (d/step))))

  ;; With shift
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 0.1234M)
           (assoc-in [:registers 10] 0.001M)
           (assoc :tapes [[0.8000 3.1020M]])
           (assoc :pc 1)
           (d/step)
           (d/step)
           (get-registers [0 10]))
         [0.1234M -1.233M]))
  )

(deftest subtract-clear
  ;; Successful op
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 1.234M)
           (assoc-in [:registers 10] 0.001M)
           (assoc :tapes [[4.1020M]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [0M -1.233M]))

  ; Into accumulator
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 1.234M)
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[4.1009M]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         -1.2339998889M))

  ; From accumulator
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 10] 1.3333333M)
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[4.0920M]])
           (assoc :pc 1)
           (d/step)
           (get-registers [10]))
         [1.3333331M])) ; <-- is this right?

  ;; Clear accumulator
  (is (= (->
           m/initial-machine-state
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[4.0900M]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0M))

  ;; Overflow and underflow
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:registers 0] 8.0M)
                 (assoc-in [:registers 10] -2.0M)
                 (assoc :tapes [[4.1020M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:registers 0] -8.0M)
                 (assoc-in [:registers 10] 3.0M)
                 (assoc :tapes [[4.1020M]])
                 (assoc :pc 1)
                 (d/step))))

  ;; Invalid stores
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[4.1011M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[4.2029M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[4.0000M]])
                 (assoc :pc 1)
                 (d/step))))
  )

(deftest multiply

  ; Successful multiply
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 1.234M)
           (assoc-in [:registers 10] 0.001M)
           (assoc :tapes [[5.1020M]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0.001234M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] -0.0000005M)
           (assoc-in [:registers 10] 0.005M)
           (assoc :tapes [[5.1020M]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         -0.0000000025M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] -0.0000001M)
           (assoc-in [:registers 10] -0.0000001M)
           (assoc :tapes [[5.1020M]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0.00000000000001M))

  ; Receiving address is cleared, sending address isn't cleared
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] -0.0000001M)
           (assoc-in [:registers 10] -0.0000001M)
           (assoc :tapes [[5.1020M]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [-0.0000001M 0M]))

  ; A non-empty accumulator has the value added
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 1.234M)
           (assoc-in [:registers 10] 0.001M)
           (assoc :accumulator 1.5M)
           (assoc :tapes [[5.1020M]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         1.501234M))

  ; Overflow
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:registers 0] -4M)
                 (assoc-in [:registers 10] -3M)
                 (assoc :tapes [[5.1020M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:registers 0] 4M)
                 (assoc-in [:registers 10] 3M)
                 (assoc :tapes [[5.1020M]])
                 (assoc :pc 1)
                 (d/step))))

  ; Addresses must not be in group 00-09
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[5.0020M]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc :tapes [[5.1001M]])
                 (assoc :pc 1)
                 (d/step))))
  )

(deftest divide)

(deftest transfer-modulus

  ; Transfer positive number
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 1.234M)
           (assoc-in [:registers 10] 0.001M)
           (assoc :tapes [[7.1020M]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [1.234M 1.235M]))

  ; Transfer negative number
  (is (= (->
           m/initial-machine-state
           (assoc-in [:registers 0] 1.234M)
           (assoc-in [:registers 10] 0.001M)
           (assoc :tapes [[7.1020M]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [1.234M 1.235M]))
  )

(deftest set-print-layout)

(deftest set-shift)

(deftest search-tape)

(deftest transfer-control)

(deftest sign-examination)

(deftest signal)
