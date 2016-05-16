(ns witch.decode
  (:require [witch.machine :as m]
            [witch.alu :as a]
            [clojure.pprint :as pp]))

; Values to multiply by when performing shifting
(def shift-values
  [0M 10M, 1M, 0.1M, 0.01M, 0.001M, 0.0001M, 0.00001M, 0.000001M, 0.0000001M])

; Keywords used by the block markers
(def block-keywords
  [:block0 :block1 :block2 :block3 :block4
   :block5 :block6 :block7 :block8 :block9])

(defn invalid-stores
  "Check that a pair of stores are ok for an ALU order"
  [src dst]
  (if (or (>= src 10) (>= dst 10))
    (= (quot src 10) (quot dst 10))
    (not (some
           #(= dst %)
           (case src
             0 [9]
             (1 2 3 4 5 6 7) [0 9]
             (8 9) [0 1 2 3 4])))))

(defn address-a
  "Extract the 'src' field from the order (digits 2,3)"
  [opcode]
  (-> opcode (* 100M) (mod 100M) int))

(defn address-b
  "Extract the 'dst' field from the order (digits 4,5)"
  [opcode]
  (-> opcode (* 10000M) (mod 100M) int))

(defn exec-add
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (->
    machine-state
    (m/read-src-address a)
    (m/read-dst-address b)
    (a/apply-shift)
    (a/add)
    (m/write-address b)
    (assoc :shift-value 1)
    (m/advance-pc)))

(defn exec-add-and-clear
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))
  
  (->
    machine-state
    (m/read-src-address a)
    (m/read-dst-address b)
    (a/add)
    (m/write-address b)
    (m/clear-address a)
    (m/advance-pc)))

(defn exec-subtract
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (->
    machine-state
    (m/read-src-address a)
    (m/read-dst-address b)
    (a/apply-shift)
    (a/subtract)
    (m/write-address b)
    (assoc :shift-value 1)
    (m/advance-pc)))

(defn exec-subtract-and-clear
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (->
    machine-state
    (m/read-src-address a)
    (m/read-dst-address b)
    (a/subtract)
    (m/write-address b)
    (m/clear-address a)
    (m/advance-pc)))

(defn exec-multiply
  [machine-state a b]
  (when (or (invalid-stores a b)
            (< a 10)
            (< b 10))
    (throw (ex-info "Invalid stores" machine-state)))

  (->
    machine-state
    (m/read-src-address a)
    (m/read-dst-address b)
    (a/multiply)
    (a/result-to-src)
    (m/read-dst-address 9)
    (a/add)
    (m/write-address 9)
    (m/clear-address b)
    (m/advance-pc)))

; todo remainder in accumulator
(defn exec-divide
  [machine-state a b]
  (when (or (invalid-stores a b)
            (< a 10)
            (< b 10))
    (throw (ex-info "Invalid stores" machine-state)))

  (->
    machine-state
    (m/read-src-address a)
    (m/read-dst-address 9)
    (a/divide)
    (m/write-address b)
    (m/clear-address 9)
    (m/advance-pc)))

(defn exec-transfer-positive-modulus
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (->
    machine-state
    (m/read-src-address a)
    (m/read-dst-address b)
    (update :alu-src a/abs)
    (a/add)
    (m/write-address b)
    (assoc :shift-value 1)
    (m/advance-pc)))

; Control instruction decodes

(defn exec-signal
  [machine-state a]
  (when (> (mod a 10) 2)
    (throw (ex-info "Invalid signal value" machine-state)))

  (pp/cl-format true ">>> Signal ~a <<<~%" a)

  (->
    machine-state
    (assoc :finished (> (mod a 10) 0))
    (m/advance-pc)))

(defn exec-sign-examination
  [machine-state a b]
  (as->
    machine-state $
    (m/read-src-address $ b)
    (assoc $ :sign-test (if (= 1 (mod a 10))
                          (>= (:alu-src $) 0)
                          (< (:alu-src $) 0)))
    (m/advance-pc $)))

(defn exec-transfer-control
  [machine-state a b]
  (if (or (= 1 (quot a 10)) (:sign-test machine-state))
    (assoc machine-state :pc b)
    machine-state))

(defn exec-search-tape
  [machine-state a b]
  (->
    machine-state
    (m/search-tape (mod a 10) (get block-keywords b))
    (m/advance-pc)))

(defn exec-search-tape-conditional
  [machine-state a b]
  (as->
    machine-state $
    (if (:sign-test $)
        (m/search-tape $ (mod a 10) (get block-keywords b))
        $)
    (m/advance-pc $)))

(defn exec-change-print-layout
  [machine-state a]
  (->
    machine-state
    (assoc :printing-layout (mod a 10))
    (m/advance-pc)))

(defn exec-set-shift-selection
  [machine-state a]
  (when (= (mod a 10) 0)
    (throw (ex-info "Invalid shift value" machine-state)))

  (->
    machine-state
    (assoc :shift-value (get shift-values (mod a 10)))
    (m/advance-pc)))

(defn decode-control
  "Decode a 'control' order. Control orders start with a '0'"
  [machine-state a b]
  (case (quot a 10)
    8 (exec-set-shift-selection machine-state a)
    7 (exec-change-print-layout machine-state a)
    5 (exec-search-tape-conditional machine-state a b)
    3 (exec-search-tape machine-state a b)
    2 (exec-transfer-control machine-state a b)
    1 (exec-sign-examination machine-state a b)
    0 (exec-signal machine-state a)
    (throw (ex-info (str "Cannot decode control opcode 0" a) machine-state))))

(defn decode
  "Decode an order"
  [machine-state opcode]

  (when (:trace machine-state)
    (pp/cl-format true "Executing ~,4F on:~%" opcode)
    (m/dump-machine-state machine-state)
    (pp/cl-format true "------------------~%"))

  (let [o (int opcode)
        a (address-a opcode)
        b (address-b opcode)]
    (case o
      1 (exec-add machine-state a b)
      2 (exec-add-and-clear machine-state a b)
      3 (exec-subtract machine-state a b)
      4 (exec-subtract-and-clear machine-state a b)
      5 (exec-multiply machine-state a b)
      6 (exec-divide machine-state a b)
      7 (exec-transfer-positive-modulus machine-state a b)
      0 (decode-control machine-state a b)
      (throw (ex-info (str "Cannot decode opcode " o) machine-state)))))

(defn step
  "Fetch and decode one order"
  [machine-state]
  (as->
    machine-state $
    (m/read-src-address $ (:pc $))
    (decode $ (:alu-src $))
    (m/verify-machine-state $)))

(defn run
  "Run the machine until it reaches a terminating instruction"
  [machine-state]
  (loop [m machine-state]
    (if-not (:finished m)
      (recur (step m))
      (identity m))))

