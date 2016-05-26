(ns witch.alu
  (:require [witch.nines :as n]))

(defn abs
  [v]
  (max v (- v)))

(defn result-to-src
  "Use the result of a previous ALU operation as the source operand for the next"
  [machine-state]
  (assoc machine-state :alu-src (:alu-result machine-state)))

(defn apply-alu-op
  "Apply an arithmetic operation to the ALU src and dst operands"
  [machine-state fn]
  (assoc machine-state
    :alu-result
    (fn
      (:alu-dst machine-state)
      (:alu-src machine-state))))

(defn apply-shift
  "Shift the source operand by the shift index"
  [machine-state]
  (update machine-state :alu-src n/shift (:shift-value machine-state)))

(defn add
  "Adds ALU operands"
  [machine-state]
  (apply-alu-op machine-state n/add))

(defn subtract
  "Subtracts ALU operands"
  [machine-state]
  (apply-alu-op machine-state n/subtract))

(defn multiply
  "Multiplies ALU operands"
  [machine-state]
  (assoc machine-state
    :alu-result
    (n/multiply
      (:alt-src machine-state)
      (:alu-dst machine-state)
      (:accumulator machine-state))))

(defn divide
  "Divides ALU operands"
  [machine-state]
  (apply-alu-op machine-state n/divide))

