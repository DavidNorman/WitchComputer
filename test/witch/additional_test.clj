(ns witch.additional-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [witch.assembler :as assembler]
            [witch.core :as core]
            [witch.decode :as d]
            [witch.machine :as m]
            [witch.nines :as n]
            [witch.tape-reader :as tape]))

(defn machine-with-stores
  [& values]
  (assoc m/initial-machine-state
         :stores (into (vec values)
                       (repeat (- 90 (count values)) 0.0000000M))))

(defn run-program
  [program]
  (let [assembled (java.io.File/createTempFile "witch-program-" ".tape")]
    (try
      (assembler/assemble-file (str "examples/" program ".witch")
                               (.getPath assembled))
      (let [result (atom nil)
            output (with-out-str
                     (reset! result
                             (-> m/initial-machine-state
                                 (assoc :tapes
                                        (tape/read-tapes [(.getPath assembled)]))
                                 (d/exec-search-tape 1 1)
                                 (d/exec-transfer-control 21 1)
                                 (d/run))))]
        {:state @result :output output})
      (finally
        (.delete assembled)))))

(deftest argument-parsing
  (is (= {:files '("tape") :single-step false :trace false}
         (core/parse-args ["tape"])))
  (is (= {:files '("first" "second") :single-step false :trace true}
         (core/parse-args ["-t" "first" "second"])))
  (is (= {:files '("tape") :single-step true :trace true}
         (core/parse-args ["tape" "-s" "-t"])))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Unknown option"
                        (core/parse-args ["--wat" "tape"])))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"required"
                        (core/parse-args []))))

(deftest main-reports-command-line-errors
  (is (str/includes? (with-out-str (core/-main)) "A tape file is required"))
  (is (str/includes? (with-out-str (core/-main "--wat" "tape"))
                     "Unknown option: --wat"))
  (is (str/includes? (with-out-str (core/-main "__missing_tape__"))
                     "__missing_tape__")))

(deftest execution-loop
  (let [stopped (assoc m/initial-machine-state :finished true)]
    (is (identical? stopped (d/run stopped))))
  (let [state (-> m/initial-machine-state
                  (assoc :tapes [[[nil 0.0100M]]])
                  (assoc :pc 1)
                  (d/run))]
    (is (:finished state))
    (is (= 1 (:pc state))))
  (let [state (-> m/initial-machine-state
                  (assoc-in [:stores 0] 0.0100M)
                  (assoc :pc 10)
                  (d/step))]
    (is (:finished state))
    (is (= 11 (:pc state))))
  (let [plain (-> m/initial-machine-state
                  (assoc :tapes [[[nil 0.0100M]]])
                  (assoc :pc 1))
        traced (assoc plain :trace true)
        plain-result (atom nil)
        traced-result (atom nil)
        trace-output (with-out-str
                       (reset! traced-result (d/step traced)))
        _ (with-out-str (reset! plain-result (d/step plain)))]
    (is (str/includes? trace-output "Executing"))
    (is (= (dissoc @plain-result :trace)
           (dissoc @traced-result :trace)))))

(deftest single-step-control
  (let [initial (-> m/initial-machine-state
                    (assoc :tapes [[[nil 0.0000M] [nil 0.0100M]]])
                    (assoc :pc 1))
        result (atom nil)
        output (binding [*in* (java.io.BufferedReader. (java.io.StringReader. "\n\n"))]
                 (with-out-str
                   (reset! result (core/run-single-step initial))))]
    (is (:finished @result))
    (is (= 2 (count (re-seq #"\[single step\]" output))))
    (is (str/includes? output "Signal 0"))
    (is (str/includes? output "Signal 1")))
  (let [initial (-> m/initial-machine-state
                    (assoc :tapes [[[nil 0.0000M] [nil 0.0100M]]])
                    (assoc :pc 1))
        result (atom nil)
        output (binding [*in* (java.io.BufferedReader. (java.io.StringReader. "\nq\n"))]
                 (with-out-str
                   (reset! result (core/run-single-step initial))))]
    (is (false? (:finished @result)))
    (is (= 0.0100M (-> @result :tapes first first second)))
    (is (str/includes? output "execution paused")))
  (let [initial (-> m/initial-machine-state
                    (assoc :tapes [[[nil 0.0100M]]])
                    (assoc :pc 1))
        result (atom nil)
        output (binding [*in* (java.io.BufferedReader. (java.io.StringReader. "not-a-button\nq\n"))]
                 (with-out-str
                   (reset! result (core/run-single-step initial))))]
    (is (false? (:finished @result)))
    (is (str/includes? output "press Return"))))

(deftest opcode-address-decoding
  (are [opcode a b] (and (= a (d/address-a opcode))
                         (= b (d/address-b opcode)))
    1.1234M 12 34
    7.9999M 99 99
    0.0100M 1 0)
  (doseq [[source valid invalid]
          [[0 [9] [0 1 8]]
           [1 [0 9] [1 2 8]]
           [8 [0 1 2 3 4] [5 8 9]]
           [12 [20 99] [10 19]]]]
    (doseq [destination valid]
      (is (false? (d/invalid-stores source destination))))
    (doseq [destination invalid]
      (is (true? (d/invalid-stores source destination)))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Cannot decode opcode"
                        (d/decode m/initial-machine-state 8.0000M)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Cannot decode control opcode"
                        (d/decode m/initial-machine-state 0.9000M))))

(deftest nines-complement-properties
  (doseq [value [-9.9999999M -5.5000000M -1.0000000M -0.0000001M
                 0.0000000M 0.0000001M 1.0000000M 5.5000000M 9.9999999M]]
    (is (= value (n/from-nines (n/to-nines value)))
        (str "round trip for " value)))
  (doseq [value [0.0000000M 0.0000001M 1.2345678M 50.0000000M 99.9999999M]]
    (is (= value (n/negate (n/negate value)))
        (str "double negation for " value)))
  (doseq [value [0M 1M 49.9999999M]]
    (is (n/positive? value)))
  (doseq [value [50M 99.9999999M]]
    (is (n/negative? value))))

(deftest arithmetic-identities
  (doseq [value [0.0000000M 1.2500000M 95.2499999M]]
    (let [state (-> (machine-with-stores value
                                          0M 0M 0M 0M 0M 0M 0M 0M 0M
                                          0.0000000M)
                    (assoc :pc 10)
                    (d/exec-add 10 20))]
      (is (= value (get-in state [:stores 10])))))
  (doseq [value [0.0000000M 1.2500000M 95.2499999M]]
    (let [state (-> (machine-with-stores value
                                          0M 0M 0M 0M 0M 0M 0M 0M 0M
                                          value)
                    (assoc :pc 10)
                    (d/exec-subtract 10 20))]
      (is (= 0M (n/from-nines (get-in state [:stores 10]))))))
  (doseq [value [1.2500000M 95.2499999M]]
    (let [state (-> (machine-with-stores value
                                          0M 0M 0M 0M 0M 0M 0M 0M 0M
                                          0.0000000M)
                    (assoc :pc 10)
                    (d/exec-add-and-clear 10 20))]
      (is (= 0M (get-in state [:stores 0])))
      (is (= value (get-in state [:stores 10])))))
  (doseq [value [1.2500000M 95.2499999M]]
    (let [state (-> (machine-with-stores value
                                          0M 0M 0M 0M 0M 0M 0M 0M 0M
                                          0.0000000M)
                    (assoc :pc 10)
                    (d/exec-transfer-positive-modulus 10 20))]
      (is (n/positive? (get-in state [:stores 10]))))))

(deftest bundled-programs
  (let [{:keys [state output]} (run-program "demo-1")]
    (is (:finished state))
    (is (= 1.6000000M (get-in state [:stores 0])))
    (is (str/includes? output "+1.5000000  +2.2500000")))
  (let [{:keys [state output]} (run-program "factorial")]
    (is (:finished state))
    (is (= 0.0362880M (get-in state [:stores 10])))
    (is (str/includes? output "+0.0000009  +0.0362880")))
  (let [{:keys [state output]} (run-program "fibonacci")]
    (is (:finished state))
    (is (= 0.0010946M (get-in state [:stores 0])))
    (is (= 0.0017711M (get-in state [:stores 10])))
    (is (str/includes? output "+0.0006765")))
  (let [{:keys [state output]} (run-program "bubble-sort")
        values (map n/from-nines (subvec (:stores state) 50 65))]
    (is (:finished state))
    (is (every? (fn [[a b]] (>= a b)) (partition 2 1 values)))
    (is (= 15 (count (re-seq #"[+-]\d\.\d{7}" output)))))
  (let [{:keys [state output]} (run-program "exercise-3")
        current (n/from-nines (get-in state [:stores 10]))
        next (n/from-nines (get-in state [:stores 20]))]
    (is (:finished state))
    (is (< (abs (- current next)) 0.000005M))
    (is (str/includes? output "+1.4142135"))))

(deftest all-bundled-programs-terminate
  (doseq [resource ["demo-2" "exercise-1" "exercise-2" "exercise-4"
                    "exercise-5" "exercise-6" "indirect" "pascal" "sine"]]
    (let [{:keys [state]} (run-program resource)]
      (is (:finished state) (str resource " should terminate"))
      (is (integer? (:pc state)) (str resource " should leave a valid PC")))))

(deftest input-and-output-addresses
  (with-redefs [rand-int (constantly 0)]
    (is (= 0M (:sending-value (m/read-sending-address m/initial-machine-state 0)))))
  (with-redefs [rand-int (constantly 1)]
    (is (= 1M (:sending-value (m/read-sending-address m/initial-machine-state 0)))))
  (let [state (assoc m/initial-machine-state :accumulator 99.12345678901234M)]
    (is (= 98.9012340M
           (:sending-value (m/read-sending-address state 8)))))
  (doseq [address [0 5 6 7 8]]
    (let [state (assoc m/initial-machine-state :transfer-output 1.0000000M)]
      (is (= state (m/write-address state address)))))
  (doseq [address [1 3]]
    (let [state (assoc m/initial-machine-state
                       :printing-layout 4
                       :transfer-output 1.2500000M)
          result (atom nil)
          output (with-out-str (reset! result (m/write-address state address)))]
      (is (= state @result))
      (is (str/includes? output "+1.2500000"))))
  (doseq [address [2 4]]
    (let [state (assoc m/initial-machine-state :transfer-output 1.2500000M)
          output (with-out-str (m/write-address state address))]
      (is (str/includes? output (str "Perforator (" address ")")))
      (is (str/includes? output "1.2500000")))))

(deftest printing-layouts
  (doseq [layout [1 2]]
    (is (= "Invalid format for printing\n"
           (with-out-str
             (m/output-printer (assoc m/initial-machine-state
                                      :printing-layout layout)
                               1 1.2500000M)))))
  (doseq [layout [3 4 5]]
    (is (str/includes?
          (with-out-str
            (m/output-printer (assoc m/initial-machine-state
                                     :printing-layout layout)
                              1 1.2500000M))
          "+1.2500000")))
  (doseq [layout [6 7 8 9]]
    (is (str/includes?
          (with-out-str
            (m/output-printer (assoc m/initial-machine-state
                                     :printing-layout layout)
                              1 1.2500000M))
          "+1.25000")))
  (doseq [[layout newlines] [[3 0] [4 1] [5 2]
                              [6 0] [7 0] [8 1] [9 2]]]
    (is (= newlines
           (count
             (filter #{\newline}
                     (with-out-str
                       (m/output-printer
                         (assoc m/initial-machine-state :printing-layout layout)
                         1 1.2500000M)))))))
  (is (= 5 (count (filter #{\newline}
                           (with-out-str
                             (m/output-printer
                               (assoc m/initial-machine-state :printing-layout 0)
                               1 1.2500000M)))))))

(deftest missing-tape-data-fails-fast
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Tape not available"
                        (m/search-tape m/initial-machine-state 1 :block1)))
  (let [state (assoc m/initial-machine-state
                     :tapes [[[nil 1M] [:block2 2M]]])]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Block marker not found"
                          (m/search-tape state 1 :block1)))))
