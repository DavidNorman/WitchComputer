(ns witch.machine
  (:require [clojure.pprint :as pp]))


(def initial-machine-state
  {:registers       (into [] (repeat 90 0))
   :accumulator     0
   :shift-value     1
   :printing-layout 0
   :pc              0
   :sign-test       :false
   :tapes           [[]]
   :finished        false})


(defn round-places
  [x places]
  "Round a bigdec to a certain number of decimal places"
  (let [factor (apply * (repeat places 10M))]
    (->
      x
      (* factor)
      (bigint)
      (bigdec)
      (/ factor))))

(defn rotate
  "Rotate a sequence fn by 1"
  [seq]
  (take (count seq) (drop 1 (cycle seq))))

(defn verify-machine-state
  "Verify that the machine state is valid"
  [machine-state]
  (when (some #(>= % +10M) (:registers machine-state))
    (throw (ex-info "Illegal machine state: register >= 10" machine-state)))
  (when (some #(<= % -10M) (:registers machine-state))
    (throw (ex-info "Illegal machine state: register <= -10\n" machine-state)))
  machine-state)

; Input from tape

(defn search-tape
  "Search the tape for a given block marker.  Leaves the tape ready to read the
  order or value following the marker."
  [machine-state tape-num marker]
  (loop [tape (get-in machine-state [:tapes (dec tape-num)])]
    (if-not (= marker (first tape))
      (recur (rotate tape))
      (assoc-in machine-state [:tapes (dec tape-num)] (rotate tape)))))

; Debug

(defn dump-machine-state
  [machine-state]
  (pp/cl-format true "~a~%" machine-state)
  machine-state)


; Special input sources

(defn input-roundoff
  [machine-state]
  [(rand-int 2) machine-state])

(defn input-tape
  [machine-state address]
  (when (> address (count (:tapes machine-state)))
    (throw (ex-info "Tape not available" machine-state)))

  [(first (get-in machine-state [:tapes (dec address)]))
   (update-in machine-state [:tapes (dec address)] rotate)])

(defn input-last-7-digits-accumultor
  [machine-state]
  [(->
     (:accumulator machine-state)
     (* 10000000M)
     (rem 1M)
     (* 10M))
   machine-state])

(defn input-accumulator
  [machine-state]
  [(:accumulator machine-state) machine-state])

(defn input-register
  [machine-state address]
  [(get (:registers machine-state) (- address 10)) machine-state])

; Special write destinations

(defn output-drain
  [machine-state]
  machine-state)

(defn output-printer
  [machine-state value]
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
  [machine-state]
  machine-state)

(defn output-accumulator
  [machine-state value]
  (when (or (>= value 10) (<= value -10M))
    (throw (ex-info "Value out of range" machine-state)))
  (assoc machine-state :accumulator (round-places value 14)))

(defn output-register
  [machine-state address value]
  (when (>= address 100)
    (throw (ex-info "Register out of range" machine-state)))
  (when (or (>= value 10) (<= value -10M))
    (throw (ex-info "Value out of range" machine-state)))
  (assoc-in machine-state [:registers (- address 10)] (round-places value 7)))

; General read and write

(defn read-src-address
  [machine-state address]
  (case address
    0 (input-roundoff machine-state)
    (1 2 3 4 5 6 7) (input-tape machine-state address)
    8 (input-last-7-digits-accumultor machine-state)
    9 (input-accumulator machine-state)
    (input-register machine-state address)))

(defn read-dst-address
  [machine-state address]
  (case address
    (0 1 2 3 4 5 6 7 8) [0M machine-state]
    9 (input-accumulator machine-state)
    (input-register machine-state address)))

(defn write-address
  [machine-state address value]
  (case address
    0 (output-drain machine-state)
    (1 3) (output-printer machine-state value)
    (2 4) (output-perforator machine-state address value)
    (5 6 7 8) (output-spare machine-state)
    9 (output-accumulator machine-state value)
    (output-register machine-state address value)))

(defn advance-pc
  [machine-state]
  (let [pc (:pc machine-state)]
    (assoc machine-state :pc (if (>= pc 10) (inc pc) pc))))


