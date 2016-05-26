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

(defn carry-over
  "Add the nines complement overflow back into the result"
  [a m n]
  (let [carry (.setScale (quot a (pow10 m)) 0)]
    (->
      a
      (+ (.movePointLeft carry n))
      (mod (pow10 m)))))

(defn negate
  "Find the nines complement negative of a number"
  [a]
  (- 100M a (.movePointLeft 1.0M (.scale a))))

(defn add
  "Add together 2 nines complement numbers"
  [a b]
  (carry-over (+ a b) 2 (.scale a)))

(defn subtract
  [a b]
  (carry-over (+ a (negate b)) 2 (.scale a)))

(defn sign-extend
  "Sign extend a representation of a store to
  prepare for multiplication"
  [x]
  (let [sign (bigint (quot x 10M))]
    (+
      (bigint (.movePointRight x 10))
      (* sign 111))))

(defn multiply
  [src dst acc]
  (let [isrc (sign-extend src)
        idst (sign-extend dst)]
    (.movePointLeft (bigdec (* isrc idst)) 20)))

(defn divide
  [a b]
  (to-nines
    (with-precision 14
      (/
        (from-nines a)
        (from-nines b)))))

(defn positive?
  [a]
  (< a 50M))

(def negative? (comp not positive?))

(defn shift
  [a factor]
  (->
    a
    (+ (* (quot a 10M) 111111100M))
    (+ (.movePointLeft (quot a 10M) (inc (.scale a))))
    (* factor)
    (adjust-places (.scale a))))

