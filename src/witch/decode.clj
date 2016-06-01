(ns witch.decode
  "Decode and process the instructions.

  See http://www.computerconservationsociety.org/witch5.htm for a fairly
  comprehensive description of the arithmetic operations."

  (:require [witch.machine :as m]
            [witch.utils :as u]
            [clojure.pprint :as pp]
            [witch.nines :as n]))

; Values to multiply by when performing shifting
(def shift-values
  [0M 1M, 0M, -1M, -2M, -3M, -4M, -5M, -6M, -7M])

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
    (assoc :sending-clear false)
    (assoc :transfer-complement false)
    (m/read-sending-address a)
    (m/transfer)
    (m/write-address b)
    (assoc :transfer-shift 1)
    (m/advance-pc)))

(defn exec-add-and-clear
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))
  
  (->
    machine-state
    (assoc :sending-clear true)
    (assoc :transfer-complement false)
    (m/read-sending-address a)
    (m/transfer)
    (m/write-address b)
    (m/advance-pc)))

(defn exec-subtract
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (->
    machine-state
    (assoc :sending-clear false)
    (assoc :transfer-complement true)
    (m/read-sending-address a)
    (m/transfer)
    (m/write-address b)
    (assoc :transfer-shift 1)
    (m/advance-pc)))

(defn exec-subtract-and-clear
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (->
    machine-state
    (assoc :sending-clear true)
    (assoc :transfer-complement true)
    (m/read-sending-address a)
    (m/transfer)
    (m/write-address b)
    (m/advance-pc)))

(defn exec-multiply-step
  "One stage of the long multiplication.  In each stage
  the multiplicand is added (or subtracted) from the accumulator
  N times, where N is the units digit of the multiplier. The shift
  is set to the step number.  Following this, the multiplier is
  shifted one digit to the left."
  [a b machine-state step]
  (as->
    machine-state $

    ; First perform the summations
    (assoc $ :sending-clear false)
    (assoc $ :transfer-complement (:muldiv-complement $))
    (assoc $ :transfer-shift (- step))

    (u/apply-while
      #(-> %
           (m/read-sending-address a)
           (m/transfer)
           (m/write-address 9)
           (m/inc-dec-register-tube b step (:muldiv-complement %)))
      $
      #(not= (m/read-register-tube % b step) 0M))

    ; Step back one place if stepping the register forwards
    (if (:muldiv-complement $)
      (->
        $
        (assoc :transfer-complement (not (:muldiv-complement $)))
        (m/read-sending-address a)
        (m/transfer)
        (m/write-address 9))
      $)))

(defn exec-multiply
  "Perform long multiplication on 2 values into the accumulator."
  [machine-state a b]
  (when (or (invalid-stores a b)
            (< a 10)
            (< b 10))
    (throw (ex-info "Invalid stores" machine-state)))

  (as->
      machine-state $
      (assoc $ :muldiv-complement
               (n/negative? (:sending-value (m/read-sending-address machine-state b))))
      (m/clear-sign $ b)

      (reduce (partial exec-multiply-step a b) $ (range 8))
      (m/advance-pc $)))

; TODO divide properly
(defn exec-divide
  [machine-state a b]
  (when (or (invalid-stores a b)
            (< a 10)
            (< b 10))
    (throw (ex-info "Invalid stores" machine-state)))

  ; Division is performed by a sequence of the same form, i.e., multiple transfer,
  ; single transfer, operation of the shift; the quotient is built up digit by digit
  ; in the register by moving the discharge one step forward or backward in the
  ; register for each transfer. For a divisor and dividend of the same sign, the
  ; divisor is subtracted from the dividend and the register moved forward until
  ; the sign of the dividend changes, when the pulse generator gives the finish
  ; signal. At this point one subtraction too many has been performed, and this is
  ; corrected by making one addition and moving the register back one step, When
  ; the divisor and dividend are of opposite sign, the multiple transfers are
  ; additions, with the register being moved back step by step and the single transfer
  ; is a subtraction. In this case it is not necessary to move on the register during
  ; the single transfer, since the fact that it has started from 0 rather than 9,
  ; the correct starting point for a complement means that the necessary correction
  ; has already been made.

  (as->
    machine-state $
    (assoc $ :muldiv-complement (=
                                  (n/sign (:sending-value (m/read-sending-address machine-state a)))
                                  (n/sign (:sending-value (m/read-sending-address machine-state 9)))))

    (m/read-sending-address $ a)
    (m/advance-pc $)))

(defn exec-transfer-positive-modulus
  [machine-state a b]
  (when (invalid-stores a b)
    (throw (ex-info "Invalid stores" machine-state)))

  (->
    machine-state
    (assoc :sending-clear false)
    (assoc :transfer-complement :sending)
    (m/read-sending-address a)
    (m/transfer)
    (m/write-address b)
    (assoc :transfer-shift 1)
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
  (when-not (#{1 2} (mod a 10))
    (throw (ex-info "Invalid sign examination opcode" machine-state)))

  (as->
    machine-state $
    (m/read-sending-address $ b)
    (assoc $ :sign-test (if (= 1 (mod a 10))
                          (n/positive? (:sending-value $))
                          (n/negative? (:sending-value $))))
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
    (assoc :transfer-shift (get shift-values (mod a 10)))
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
    (m/read-sending-address $ (:pc $))
    (decode $ (:sending-value $))))

(defn run
  "Run the machine until it reaches a terminating instruction"
  [machine-state]
  (loop [m machine-state]
    (if-not (:finished m)
      (recur (step m))
      (identity m))))

