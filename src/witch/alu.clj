(ns witch.alu)

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
    (with-precision 15
      (fn
        (:alu-dst machine-state)
        (:alu-src machine-state)))))

(defn apply-shift
  "Shift the source operand by the shift index"
  [machine-state]
  (update machine-state :alu-src * (:shift-value machine-state)))

(defn add
  "Adds ALU operands"
  [machine-state]
  (apply-alu-op machine-state +))

(defn subtract
  "Subtracts ALU operands"
  [machine-state]
  (apply-alu-op machine-state -))

(defn multiply
  "Multiplies ALU operands"
  [machine-state]
  (apply-alu-op machine-state *))

(defn divide
  "Divides ALU operands"
  [machine-state]
  (apply-alu-op machine-state /))

