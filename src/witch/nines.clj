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

(defn pow10
  "Generate 10 to the power of X"
  [x]
  (apply * (repeat x 10M)))

(defn round-places
  [x places]
  "Round a bigdec to a certain number of decimal places"
  (let [factor (pow10 places)]
    (->
      x
      (mod 100M)
      (* factor)
      (bigint)
      (bigdec)
      (/ factor))))

(defn carry-over
  "Add the nines complement overflow back into the result"
  [a m n]
  (+ (/ (quot a (pow10 m)) (pow10 n)) (mod a 100M)))

(defn negate
  "Find the nines complement negative of a number"
  [a]
  (- 99.9999999M a))

(defn add
  "Add together 2 nines complement numbers"
  [a b]
  (carry-over (+ a b) 2 7))

(defn subtract
  [a b]
  (carry-over (+ a (negate b)) 2 7))

(defn multiply
  [a b]
  (let [ext-a (+ a (* (quot a 10M) 111111100.00000001111111M))
        ext-b (+ b (* (quot b 10M) 111111100.00000001111111M))]
    (->
      (* ext-a ext-b)
      (carry-over 9 14)
      (round-places 14))))

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

(defn shift
  [a factor]
  (let [sign-extend (* (quot a 10M) 111111100M)]
    (->
      a
      (+ sign-extend)
      (* factor)
      (round-places 7))))

