(ns witch.assembler
  "Translate symbolic WITCH tape source into the numerical tape format."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def label-pattern #"(?i)^([a-z][a-z0-9_-]*)\s*=\s*(\d{1,2})$")
(def raw-order-pattern #"^\d{5}$")
(def data-pattern #"^[-+]\d{8}$")
(def block-pattern #"^#\d$")

(defn assembler-error
  [message line-number line]
  (throw (ex-info message {:line-number line-number :line line})))

(defn parse-number
  [token description line-number line]
  (try
    (Integer/parseInt token)
    (catch NumberFormatException _
      (assembler-error (str "Invalid " description ": " token)
                       line-number line))))

(defn parse-address
  [labels token line-number line]
  (let [address (or (get labels (str/upper-case token))
                    (when (re-matches #"\d{1,2}" token)
                      (parse-number token "address" line-number line)))]
    (when (nil? address)
      (assembler-error (str "Unknown address or label: " token)
                       line-number line))
    address))

(defn expect-arity
  [mnemonic operands expected line-number line]
  (when-not (= expected (count operands))
    (assembler-error
      (format "%s expects %d operand%s, got %d"
              mnemonic expected (if (= expected 1) "" "s") (count operands))
      line-number line)))

(defn order
  [opcode a b]
  (format "%d%02d%02d" opcode a b))

(defn main-order
  [opcode mnemonic labels operands line-number line]
  (expect-arity mnemonic operands 2 line-number line)
  (order opcode
         (parse-address labels (first operands) line-number line)
         (parse-address labels (second operands) line-number line)))

(defn one-address-control
  [a mnemonic labels operands line-number line]
  (expect-arity mnemonic operands 1 line-number line)
  (order 0 a (parse-address labels (first operands) line-number line)))

(def shift-selectors
  {1 1, 0 2, -1 3, -2 4, -3 5, -4 6, -5 7, -6 8, -7 9})

(defn assemble-instruction
  [labels text line-number line]
  (let [[raw-mnemonic & operands] (str/split text #"\s+")
        mnemonic (-> raw-mnemonic str/upper-case (str/replace "_" "-"))]
    (case mnemonic
      "ADD" (main-order 1 mnemonic labels operands line-number line)
      ("ADD-AND-CLEAR" "ADD-CLEAR")
      (main-order 2 mnemonic labels operands line-number line)
      "SUBTRACT" (main-order 3 mnemonic labels operands line-number line)
      ("SUBTRACT-AND-CLEAR" "SUBTRACT-CLEAR")
      (main-order 4 mnemonic labels operands line-number line)
      "MULTIPLY" (main-order 5 mnemonic labels operands line-number line)
      "DIVIDE" (main-order 6 mnemonic labels operands line-number line)
      ("TRANSFER-POSITIVE-MODULUS" "TRANSFER-MODULUS")
      (main-order 7 mnemonic labels operands line-number line)

      "CLEAR"
      (do
        (expect-arity mnemonic operands 1 line-number line)
        (order 2 (parse-address labels (first operands) line-number line) 0))

      "SIGNAL"
      (do
        (expect-arity mnemonic operands 1 line-number line)
        (let [signal (parse-number (first operands) "signal" line-number line)]
          (when-not (<= 0 signal 2)
            (assembler-error "Signal must be 0, 1, or 2" line-number line))
          (order 0 signal 0)))

      ("FINISH" "STOP")
      (do
        (expect-arity mnemonic operands 0 line-number line)
        (order 0 1 0))

      "TEST-POSITIVE" (one-address-control 11 mnemonic labels operands line-number line)
      "TEST-NEGATIVE" (one-address-control 12 mnemonic labels operands line-number line)
      ("TRANSFER-CONTROL" "JUMP")
      (one-address-control 21 mnemonic labels operands line-number line)
      ("TRANSFER-CONTROL-IF" "JUMP-IF")
      (one-address-control 22 mnemonic labels operands line-number line)

      "SEARCH"
      (do
        (expect-arity mnemonic operands 2 line-number line)
        (let [block (parse-number (first operands) "block number" line-number line)
              tape (parse-address labels (second operands) line-number line)]
          (when-not (<= 0 block 9)
            (assembler-error "Block number must be between 0 and 9" line-number line))
          (order 0 (+ 30 block) tape)))

      "SEARCH-IF"
      (do
        (expect-arity mnemonic operands 2 line-number line)
        (let [block (parse-number (first operands) "block number" line-number line)
              tape (parse-address labels (second operands) line-number line)]
          (when-not (<= 0 block 9)
            (assembler-error "Block number must be between 0 and 9" line-number line))
          (order 0 (+ 50 block) tape)))

      "PRINT-LAYOUT"
      (do
        (expect-arity mnemonic operands 1 line-number line)
        (let [layout (parse-number (first operands) "print layout" line-number line)]
          (when-not (<= 0 layout 9)
            (assembler-error "Print layout must be between 0 and 9" line-number line))
          (order 0 (+ 70 layout) 0)))

      "PRINT-LAYOUT-1"
      (do
        (expect-arity mnemonic operands 0 line-number line)
        (order 0 73 0))

      "PRINT-LAYOUT-2"
      (do
        (expect-arity mnemonic operands 0 line-number line)
        (order 0 74 0))

      "SHIFT"
      (do
        (expect-arity mnemonic operands 1 line-number line)
        (let [amount (parse-number (first operands) "shift" line-number line)
              selector (get shift-selectors amount)]
          (when-not selector
            (assembler-error "Shift must be between -7 and 1" line-number line))
          (order 0 (+ 80 selector) 0)))

      (assembler-error (str "Unknown order: " raw-mnemonic) line-number line))))

(defn source-parts
  [line]
  (let [[code comment] (str/split line #";" 2)]
    {:code (str/trim code)
     :comment comment}))

(defn collect-labels
  [lines]
  (reduce
    (fn [labels [index line]]
      (let [{:keys [code]} (source-parts line)]
        (if-let [[_ name value] (re-matches label-pattern code)]
          (let [key (str/upper-case name)]
            (when (contains? labels key)
              (assembler-error (str "Duplicate label: " name) (inc index) line))
            (assoc labels key (parse-number value "address" (inc index) line)))
          labels)))
    {}
    (map-indexed vector lines)))

(defn assemble-line
  [labels line-number line]
  (let [{:keys [code comment]} (source-parts line)
        emitted-comment (when comment (str ";" comment))]
    (cond
      (empty? code) [line]
      (re-matches label-pattern code) [(str "; " code)]
      (= code "==tape") (cond-> [code] emitted-comment (conj emitted-comment))
      (re-matches block-pattern code) (cond-> [code] emitted-comment (conj emitted-comment))
      (re-matches raw-order-pattern code) (cond-> [code] emitted-comment (conj emitted-comment))
      (re-matches data-pattern code) (cond-> [code] emitted-comment (conj emitted-comment))
      :else (cond-> [(assemble-instruction labels code line-number line)]
              emitted-comment (conj emitted-comment)))))

(defn assemble-lines
  [lines]
  (let [lines (vec lines)
        labels (collect-labels lines)]
    (vec
      (mapcat
        (fn [[index line]] (assemble-line labels (inc index) line))
        (map-indexed vector lines)))))

(defn assemble-string
  [source]
  (str (str/join "\n" (assemble-lines (str/split-lines source))) "\n"))

(defn assemble-file
  [input-file output-file]
  (when (= (.getCanonicalPath (io/file input-file))
           (.getCanonicalPath (io/file output-file)))
    (throw (ex-info "Input and output files must be different"
                    {:input input-file :output output-file})))
  (spit output-file (assemble-string (slurp input-file)))
  output-file)

(defn -main
  [& args]
  (when-not (= 2 (count args))
    (throw (ex-info
             "Usage: clojure -M:assemble <symbolic-source> <output-tape>"
             {:args args})))
  (let [[input-file output-file] args]
    (assemble-file input-file output-file)
    (println (str "Assembled " input-file " -> " output-file))))
