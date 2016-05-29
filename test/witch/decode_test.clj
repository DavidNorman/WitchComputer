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

#_(deftest add

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

#_(deftest add-clear

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

#_(deftest subtract
  ;; Successful op
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc-in [:stores 10] (n/to-nines 0.001M))
           (assoc :tapes [[[nil 3.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [(n/to-nines 1.234M) (n/to-nines -1.233M)]))

  ; Into accumulator TODO fix this
  #_(is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc :accumulator 00.00000011110000M)
           (assoc :tapes [[[nil 3.1009M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         98.76600011100000M))

  ; From accumulator TODO fix this (is this right?)
  #_(is (= (->
           m/initial-machine-state
           (assoc-in [:stores 10] (n/to-nines 1.3333333M))
           (assoc :accumulator 00.00000011110000M)
           (assoc :tapes [[[nil 3.0920M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [10]))
         [(n/to-nines 1.3333331M)]))

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

#_(deftest subtract-clear
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

  ; Into accumulator TODO fix this
  #_(is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[[nil 4.1009M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         -1.2339998889M))

  ; From accumulator TODO fix this, is it right?
  #_(is (= (->
           m/initial-machine-state
           (assoc-in [:stores 10] (n/to-nines 1.3333333M))
           (assoc :accumulator 0.0000001111M)
           (assoc :tapes [[[nil 4.0920M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [10]))
         [(n/to-nines 1.3333331M)]))

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

#_(deftest multiply

  ; Successful multiply
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc-in [:stores 10] (n/to-nines 0.001M))
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0.001234M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines -0.0000005M))
           (assoc-in [:stores 10] (n/to-nines 0.005M))
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         -0.0000000025M))

  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines -0.0000001M))
           (assoc-in [:stores 10] (n/to-nines -0.0000001M))
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         0.00000000000001M))

  ; Receiving address is cleared, sending address isn't cleared
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines -0.0000001M))
           (assoc-in [:stores 10] (n/to-nines -0.0000001M))
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [-0.0000001M 0M]))

  ; A non-empty accumulator has the value added
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc-in [:stores 10] (n/to-nines 0.001M))
           (assoc :accumulator 1.5M)
           (assoc :tapes [[[nil 5.1020M]]])
           (assoc :pc 1)
           (d/step)
           :accumulator)
         1.501234M))

  ; Overflow
  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines -4M))
                 (assoc-in [:stores 10] (n/to-nines -3M))
                 (assoc :tapes [[[nil 5.1020M]]])
                 (assoc :pc 1)
                 (d/step))))

  (is (thrown? ExceptionInfo
               (->
                 m/initial-machine-state
                 (assoc-in [:stores 0] (n/to-nines 4M))
                 (assoc-in [:stores 10] (n/to-nines 3M))
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

#_(deftest transfer-modulus

  ; Transfer positive number
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc-in [:stores 10] (n/to-nines 0.001M))
           (assoc :tapes [[[nil 7.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [(n/to-nines 1.234M) (n/to-nines 1.235M)]))

  ; Transfer negative number ; TODO
  (is (= (->
           m/initial-machine-state
           (assoc-in [:stores 0] (n/to-nines 1.234M))
           (assoc-in [:stores 10] (n/to-nines 0.001M))
           (assoc :tapes [[[nil 7.1020M]]])
           (assoc :pc 1)
           (d/step)
           (get-registers [0 10]))
         [(n/to-nines 1.234M) (n/to-nines 1.235M)]))
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
         10M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8200M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         1M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8300M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         0.1M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8400M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         0.01M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8500M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         0.001M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8600M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         0.0001M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8700M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         0.00001M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8800M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         0.000001M))

  (is (= (->
           m/initial-machine-state
           (assoc :tapes [[[nil 0.8900M]]])
           (assoc :pc 1)
           (d/step)
           :transfer-shift)
         0.0000001M))
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
