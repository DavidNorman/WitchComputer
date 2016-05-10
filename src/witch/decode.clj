(ns witch.decode
  (:require [witch.machine :as m]
            [clojure.pprint :as pp]))

; Values to multiply by when performing shifting
(def shift-values
  [10M, 1M, 0.1M, 0.01M, 0.001M, 0.0001M, 0.00001M, 0.000001M, 0.0000001M])

; Keywords used by the block markers
(def block-keywords
  [:block0 :block1 :block2 :block3 :block4
   :block5 :block6 :block7 :block8 :block9])

(defn invalid-stores
  "Check that a pair of stores are ok for an add/subtract order"
  [src dst]
  (if (or (>= src 10) (>= dst 10))
    (= (quot src 10) (quot dst 10))
    (not (some
           #(= dst %)
           (case src
             0 [9]
             (1 2 3 4 5 6 7) [0 9]
             (8 9) [0 1 2 3 4])))))

(defn apply-shift
  "Apply the current shift value"
  [machine-state val]
  (* val (:shift-value machine-state)))

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

  (let [[src m] (m/read-src-address machine-state a)
        [dst m] (m/read-dst-address m b)]
    (->
      m
      (m/write-address b (+ dst (apply-shift m src)))
      (assoc :shift-value 1))))

(defn exec-add-and-clear
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (let [[src m] (m/read-src-address machine-state a)
        [dst m] (m/read-dst-address m b)]
    (->
      m
      (m/write-address b (+ dst src))
      (m/write-address a 0))))

(defn exec-subtract
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (let [[src m] (m/read-src-address machine-state a)
        [dst m] (m/read-dst-address m b)]
    (->
      m
      (m/write-address b (- dst (apply-shift m src)))
      (assoc :shift-value 1))))

(defn exec-subtract-and-clear
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (let [[src m] (m/read-src-address machine-state a)
        [dst m] (m/read-dst-address m b)]
    (->
      m
      (m/write-address b (- dst src))
      (m/write-address a 0))))

(defn exec-multiply
  [machine-state a b]
  (when (or (invalid-stores a b)
            (< a 10)
            (< b 10))
    (throw (ex-info "Invalid stores" machine-state)))

  (let [[src m] (m/read-dst-address machine-state a)
        [dst m] (m/read-dst-address m b)]
    (->
      m
      (m/write-address 9 (* dst src))
      (m/write-address b 0))))

(defn exec-divide
  [machine-state a b]
  (when (or (invalid-stores a b)
            (< a 10)
            (< b 10))
    (throw (ex-info "Invalid stores" machine-state)))

  (let [[src m] (m/read-dst-address machine-state a)
        [acc m] (m/input-accumulator m)]
    (->
      m
      (m/write-address b (/ acc src))
      (m/write-address 9 0))))

(defn exec-transfer-positive-modulus
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (let [[src m] (m/read-src-address machine-state a)
        [dst m] (m/read-dst-address m b)
        src (apply-shift m src)]
    (->
      m
      (m/write-address b (if (>= src 0) (+ dst src) (- dst src)))
      (assoc :shift-value 1))))

; Control instruction decodes

(defn exec-signal
  [machine-state a]
  (pp/cl-format true "Signal ~a~%" a)
  (assoc machine-state :finished true))

(defn exec-sign-examination
  [machine-state a b]
  (let [[val m] (m/read-src-address machine-state b)
        positive? (>= val 0)]
    (if (= 1 (mod a 10))
      (assoc m :sign-test positive?)
      (assoc m :sign-test (not positive?)))))

(defn exec-transfer-control
  [machine-state a b]
  (if (or (= 1 (quot a 10)) (:sign-test machine-state))
    (assoc machine-state :pc b)
    machine-state))

(defn exec-search-tape
  [machine-state a b]
  (m/search-tape machine-state (mod a 10) (get block-keywords b)))

(defn exec-search-tape-conditional
  [machine-state a b]
  (if (:sign-test machine-state)
    (m/search-tape machine-state (mod a 10) (get block-keywords b))
    machine-state))

(defn exec-change-print-layout
  [machine-state a]
  (assoc machine-state :printing-layout (mod a 10)))

(defn exec-set-shift-selection
  [machine-state a]
  (assoc machine-state :shift-value (get shift-values (mod a 10))))

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
    (pp/cl-format true "Executing ~,4F~%" opcode)
    (m/dump-machine-state machine-state))

  (if (keyword? opcode)
    (throw (ex-info (str "Cannot execute block marker " opcode) machine-state))
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
        (throw (ex-info (str "Cannot decode opcode " o) machine-state))))))

(defn step
  "Fetch and decode one order"
  [machine-state]
  (let [[opcode m] (m/read-src-address machine-state (:pc machine-state))]
    (->
      m
      (decode opcode)
      (m/advance-pc)
      (m/verify-machine-state))))

(defn run
  "Run the machine until it reaches a terminating instruction"
  [machine-state]
  (loop [m machine-state]
    (if-not (:finished m)
      (recur (step m))
      (identity m))))

