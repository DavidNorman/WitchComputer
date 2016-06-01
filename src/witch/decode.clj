(ns witch.decode
  (:require [witch.machine :as m]
            [clojure.pprint :as pp]
            [witch.nines :as n]))

; Values to multiply by when performing shifting
(def shift-values
  [0M 1M, 0M, -1M, -2M, -3M, -4M, -5M, -6M, -7M])

; Keywords used by the block markers
(def block-keywords
  [:block0 :block1 :block2 :block3 :block4
   :block5 :block6 :block7 :block8 :block9])

(defn apply-while
  "Apply a function to a value repeatedly while a predicate
  returns true for the result."
  [f x p]
  (if (p x) (recur f (f x) p) x))


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
    (assoc :transfer-complement :false)
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
    (assoc :transfer-complement :false)
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
    (assoc :transfer-complement :true)
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
    (assoc :transfer-complement :true)
    (m/read-sending-address a)
    (m/transfer)
    (m/write-address b)
    (m/advance-pc)))

(defn exec-multiply-summation
  "Apply a summation operation into the accumulator multiple times"
  [machine-state a n]
  (reduce (fn [machine-state _]
            (->
              machine-state
              (m/read-sending-address a)
              (m/transfer)
              (m/write-address 9)))
          machine-state
          (range n)))

(defn exec-multiply-step
  "One stage of the long multiplication.  In each stage
  the multiplicand is added (or subtracted) from the accumulator
  N times, where N is the units digit of the multiplier. The shift
  is set to the step number.  Following this, the multiplier is
  shifted one digit to the left."
  [a b machine-state step]
  (let [reg (m/read-destination-address machine-state b)
        units (n/units-digit reg)]
    (->
      machine-state

      ; First perform the summations
      (assoc :sending-clear false)
      (assoc :transfer-complement :muldiv)
      (assoc :transfer-shift (- step))
      (exec-multiply-summation a units)

      ; Now shift the multiplier register
      (assoc :sending-clear true)
      (assoc :transfer-complement :false)
      (assoc :transfer-shift 1M)
      (m/read-sending-address b)
      (m/transfer)
      (m/write-address b))))

(defn exec-multiply
  "Perform long multiplication on 2 values into the accumulator.

  See http://www.computerconservationsociety.org/witch5.htm for a fairly
  comprehensive description of the multiplication process."
  [machine-state a b]
  (when (or (invalid-stores a b)
            (< a 10)
            (< b 10))
    (throw (ex-info "Invalid stores" machine-state)))

  ; Where the multiplier is positive, the multiplicand is added into the
  ; accumulator N1 times where N1 is the number stored in the most significant
  ; tube of the register (multiplier). A shift to the right is now introduced
  ; by the relay shift circuit (i.e., in Fig. 6, point A is connected to b and
  ; B to c, points S and a being connected to s), and the multiplicand added
  ; into the accumulator N2 times, where N2 is the number stored in the second
  ; tube of the register. This process of multiple additions alternating with
  ; operation of the shift unit continues until the whole of the multiplier is
  ; dealt with, In order to perform the correct number of additions, the tube
  ; in the register containing the digit of the multiplier being considered is
  ; moved back one step (for convenience it is actually moved on nine steps
  ; without carry over) for each single addition, The pulse generator is arranged
  ; to give the finish signal calling for the next operation when the appropriate
  ; digit of the register has reached zero and carryover is complete.

  ; When the multiplier is a complement, a similar procedure is carried out
  ; with a few modifications. Multiple subtractions are required, and the register
  ; tube is moved forward one step for each subtraction. Since the complement of
  ; 0 is 9, it follows that transfers should stop when the register tube reaches
  ; 9, As the pulse generator allows transfers to continue until the tube reaches
  ; 0, one too many will be performed by the time the finish signal is given,
  ; and it is necessary to make a single addition to correct for this. The
  ; sequence then is multiple subtraction, single addition, operation of shift.
  ; These sequences are controlled by relays in the sequence controlling and
  ; routing section of the machine. It is, of course, only necessary to give
  ; the order "multiply"; the precise sequence required is selected before the
  ; operation starts by a relay circuit dependent on the sign of the contents
  ; of the selected addresses.

  (as->
      machine-state $
      (assoc $ :muldiv-complement
               (n/negative? (:sending-value (m/read-sending-address machine-state b))))

      (assoc $ :sending-clear true)
      (assoc $ :transfer-complement :sending)
      (m/read-sending-address $ b)
      (m/transfer $)
      (m/write-address $ b)

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

