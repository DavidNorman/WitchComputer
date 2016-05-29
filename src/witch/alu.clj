(ns witch.alu
  (:require [witch.nines :as n]))

(defn abs
  [v]
  (max v (- v)))

(defn result-to-src
  "Use the result of a previous ALU operation as the source operand for the next"
  [machine-state]
  (assoc machine-state :sending-value (:transfer-output machine-state)))

(defn apply-shift
  "Shift the source operand by the shift index"
  [machine-state]
  machine-state)

(defn add
  "Adds ALU operands"
  [machine-state]
  machine-state)

(defn subtract
  "Subtracts ALU operands"
  [machine-state]
  machine-state)

(defn multiply
  "Multiplies ALU operands"
  [machine-state]
  machine-state)

(defn divide
  "Divides ALU operands"
  [machine-state]
  machine-state)

