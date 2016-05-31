(ns witch.machine
  (:require [witch.nines :as n]
            [clojure.pprint :as pp]))


(def initial-machine-state
  {:stores              (into [] (repeat 90 0.0000000M))
   :accumulator         0.00000000000000M
   :sending-clear       false
   :transfer-shift      0M
   :transfer-complement false
   :transfer-output     0.00000000000000M
   :printing-layout     0M
   :pc                  0M
   :sign-test           :false
   :tapes               [[]]
   :finished            false})


(defn rotate
  "Rotate a sequence fn by 1"
  [seq]
  (take (count seq) (drop 1 (cycle seq))))

; Input from tape

(defn search-tape
  "Search the tape for a given block marker.  Leaves the tape ready to read the
  order or value following the marker."
  [machine-state tape-num marker]
  (loop [tape (get-in machine-state [:tapes (dec tape-num)])]
    (if-not (= marker (first (first tape)))
      (recur (rotate tape))
      (assoc-in machine-state [:tapes (dec tape-num)] tape))))

(defn read-tape
  [machine-state tape-num]
  "Read the first value off the tape."
  (->
    (get-in machine-state [:tapes (dec tape-num)])
    (first)
    (second)))

; Debug

(defn dump-machine-state
  [machine-state]
  (pp/cl-format true "~a~%" machine-state)
  machine-state)

; Transfer unit

(defn transfer-shift
  "Perform the shift part of the transfer unit"
  [x shift]
  (->
    x
    (.movePointRight shift)
    (mod 100.0M)))

(defn transfer-complement
  "Perform the complement part of the transfer unit"
  [x comp]
  (if comp (n/negate x) x))

(defn transfer
  "Perform the transfer (and transformation) of the source value.  The
   value is shifted by the shift amount, and complemented if required."
  [machine-state]
  (assoc machine-state
    :transfer-output
    (->
      (:sending-value machine-state)
      (n/sign-extend)
      (transfer-shift (:transfer-shift machine-state))
      (+ 0.00000000000000M)
      (transfer-complement (:transfer-complement machine-state)))))

; Cleared value of store/accumulator

(defn- clear
  "Produce a cleared or non-cleared result depending on the state
  of the clear controls"
  [machine-state]
  (*
    (:sending-value machine-state)
    (if (:sending-clear machine-state) 0M 1M)))

; Special input sources

(defn input-roundoff
  [machine-state _]
  (->
    machine-state
    (assoc :sending-value (rand-int 2))))

(defn input-tape
  [machine-state address]
  (when (> address (count (:tapes machine-state)))
    (throw (ex-info "Tape not available" machine-state)))

  (as->
    machine-state $
    (assoc $ :sending-value (read-tape $ address))
    (update-in $ [:tapes (dec address)] rotate)))

(defn input-last-7-digits-accumultor
  [machine-state _]
  (assoc machine-state
    :sending-value
    (->
      (:accumulator machine-state)
      (.movePointRight 7)
      (rem 1M)
      (.movePointRight 1)
      (+ (* 10M (n/sign (:accumulator machine-state))))
      (.setScale 7))))

(defn input-accumulator
  [machine-state _]
  (as-> machine-state $
        (assoc $ :sending-value (:accumulator $))
        (assoc $ :accumulator (clear $))))

(defn input-store
  [machine-state address]
  (let [a (- address 10)]
    (as-> machine-state $
          (assoc $ :sending-value (get (:stores $) a))
          (assoc-in $ [:stores a] (clear $)))))

; Special write destinations

(defn output-drain
  [machine-state _ _]
  machine-state)

(defn output-printer
  [machine-state _ value]
  (pp/cl-format
    true
    (case (:printing-layout machine-state)
      (1 2) "Invalid format for printing\n"
      3 "~12,7@F"
      4 "~12,7@F~%"
      5 "~12,7@F~2%"
      6 "~10,5@F"
      7 "~12,5@F"
      8 "~12,5@F~%"
      9 "~12,5@F~2%"
      0 "~5%") value)
  machine-state)

(defn output-perforator
  [machine-state address value]
  (pp/cl-format true "Perforator (~a) ~a~%" address value)
  machine-state)

(defn output-spare
  [machine-state _ _]
  machine-state)

(defn output-accumulator
  [machine-state _ value]
  (let [old-value (:accumulator machine-state)
        new-value (n/carry-over (+ old-value value) 14)]
    (when (not (#{0M 9M} (quot new-value 10M)))
      (throw (ex-info "Value out of range" machine-state)))
    (assoc machine-state :accumulator new-value)))

(defn output-store
  [machine-state address value]
  (when (>= address 100)
    (throw (ex-info "Register out of range" machine-state)))
  (let [a (- address 10)
        old-value (get-in machine-state [:stores a])
        truncated-value (n/adjust-places value 7)
        new-value (n/carry-over (+ old-value truncated-value) 7)]
    (when (not (#{0M 9M} (quot new-value 10M)))
      (throw (ex-info "Value out of range" machine-state)))
    (assoc-in machine-state [:stores a] new-value)))

; General read and write

(defn read-src-fn
  [address]
  (case address
    0               input-roundoff
    (1 2 3 4 5 6 7) input-tape
    8               input-last-7-digits-accumultor
    9               input-accumulator
    input-store))

(defn write-fn
  [address]
  (case address
    0         output-drain
    (1 3)     output-printer
    (2 4)     output-perforator
    (5 6 7 8) output-spare
    9         output-accumulator
    output-store))

(defn read-sending-address
  [machine-state address]
  ((read-src-fn address) machine-state address))

(defn write-address
  [machine-state address]
  ((write-fn address) machine-state address (:transfer-output machine-state)))

(defn advance-pc
  [machine-state]
  (let [pc (:pc machine-state)]
    (assoc machine-state :pc (if (>= pc 10) (inc pc) pc))))


