(ns witch.nines
  "Simulation of the 9's complement artimetic used by the WITCH machine.

   In the computer, numbers are stored in 9's complement, in either a 8
   digit store plus one sign digit, or the 15 digit accumulator plus a
   sign digit. There is a fixed decimal point after the first digit.

   In the simulation, numbers are stored in the big decimal format, with
   the sign digit prepended.  For instance, 1.5 is stored as 01.5M and
   -5.32 is stored as 94.67M"
  )

(defn to-nines
  "Convert a BigDecimal into nines complement representation"
  [x]
    (if (< x 0M)
      (+ 99.9999999M x)
      (+ 00.0000000M x)))

(defn from-nines
  "Convert a BigDecimal from nines complement representation"
  [x]
  (if (> x 10M)
    (- x 99.9999999M) x))

(defn pow10
  "Generate 10 to the power of X"
  [x]
  (.scaleByPowerOfTen 1.0M x))

(defn adjust-places
  [x places]
  "Round/extend a bigdec to a certain number of decimal places. After extension
  there will always be one digit and a sign digit to the left of the decimal point"
  (->
    x
    (.setScale places java.math.RoundingMode/DOWN)
    (mod 100M)))

(defn sign
  "Return the sign digit"
  [x]
  (.setScale (quot x 10M) 0))

(defn sign-extend-left
  "Extend sign bit out by 7 more digits"
  [x]
  (+ x (* (sign x) 111111100M)))

(defn sign-extend-right
  "Sign extend to 14 digits after the decimal point"
  [x]
  (if (= (.scale x) 7)
    (+ x (* (sign x) 0.00000001111111M))
    x))


(defn carry-over
  "Add the nines complement overflow back into the result"
  [a n]
  (let [carry-value (.setScale (quot a 100M) 0)
        carry-over (.movePointLeft carry-value n)]
    (->
      a
      (+ carry-over)
      (adjust-places n))))

(defn negate
  "Find the nines complement negative of a number"
  [a]
  (- 100M a (.movePointLeft 1M (.scale a))))

(defn positive?
  [a]
  (< a 50M))

(def negative? (comp not positive?))

; TODO remove this
(defn units-digit
  "Get the units digit of a nines complement number"
  [x]
  (mod (quot x 1M) 10M))

(defn get-digit
  "Get the n'th digit from a number"
  [x n]
  (->
    x
    (.movePointRight n)
    (rem 10M)
    (.setScale 0 java.math.RoundingMode/DOWN)))