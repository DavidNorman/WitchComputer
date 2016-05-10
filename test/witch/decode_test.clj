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

  ;; With shift


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

(deftest multiply)

(deftest divide)

(deftest transfer-modulus)

(deftest set-print-layout)

(deftest set-shift)

(deftest search-tape)

(deftest transfer-control)

(deftest sign-examination)

(deftest signal)
