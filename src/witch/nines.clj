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
  (with-precision 17
    (if (< x 0M) (+ 99.9999999M x) x)))

(defn from-nines
  "Convert a BigDecimal from nines complement representation"
  [x]
  (with-precision 17
    (if (> x 10M) (- x 99.9999999M) x)))

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

(defn carry-over
  "Add the nines complement overflow back into the result"
  [a]
  (+ (/ (quot a 100M) 10000000) (mod a 100M)))

(defn negate
  "Find the nines complement negative of a number"
  [a]
  (- 99.9999999M a))

(defn add
  [a b]
  (carry-over (+ a b)))

(defn subtract
  [a b]
  (carry-over (+ a (negate b))))

(defn multiply
  [a b]
  (to-nines
    (*
      (from-nines a)
      (from-nines b))))

(defn divide
  [a b]
  (to-nines
    (/
      (from-nines a)
      (from-nines b))))

(defn positive?
  [a]
  (< a 50M))

(defn shift
  [a factor]
  (let [sign-extend (* (quot a 10M) 111111100M)]
    (->
      a
      (+ sign-extend)
      (* factor)
      (mod 100M)
      (round-places 7))))

