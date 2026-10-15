(ns witch.assembler-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [witch.assembler :as assembler]
            [witch.tape-reader :as tape]))

(defn significant-lines
  [text]
  (->> (str/split-lines text)
       (remove #(or (str/blank? %) (str/starts-with? % ";")))
       vec))

(deftest arithmetic-orders
  (is (= ["11020" "21020" "21020" "31020" "41020" "41020"
          "51020" "61020" "71020" "71020" "21000"]
         (significant-lines
           (assembler/assemble-string
             (str "ADD 10 20\n"
                  "ADD-AND-CLEAR 10 20\n"
                  "ADD-CLEAR 10 20\n"
                  "SUBTRACT 10 20\n"
                  "SUBTRACT-AND-CLEAR 10 20\n"
                  "SUBTRACT_CLEAR 10 20\n"
                  "MULTIPLY 10 20\n"
                  "DIVIDE 10 20\n"
                  "TRANSFER-POSITIVE-MODULUS 10 20\n"
                  "TRANSFER-MODULUS 10 20\n"
                  "CLEAR 10\n"))))))

(deftest historical-control-orders
  (is (= ["01120" "01220" "02120" "02120" "02220" "02220"
          "03302" "05302" "07300" "07400" "00100" "00100"]
         (significant-lines
           (assembler/assemble-string
             (str "TEST-POSITIVE 20\n"
                  "TEST-NEGATIVE 20\n"
                  "TRANSFER-CONTROL 20\n"
                  "JUMP 20\n"
                  "TRANSFER-CONTROL-IF 20\n"
                  "JUMP-IF 20\n"
                  "SEARCH 3 02\n"
                  "SEARCH-IF 3 02\n"
                  "PRINT-LAYOUT-1\n"
                  "PRINT-LAYOUT-2\n"
                  "FINISH\n"
                  "STOP\n"))))))

(deftest simulator-control-extensions
  (is (= ["00000" "00200" "07900" "08100" "08200" "08900"]
         (significant-lines
           (assembler/assemble-string
             (str "SIGNAL 0\n"
                  "SIGNAL 2\n"
                  "PRINT-LAYOUT 9\n"
                  "SHIFT 1\n"
                  "SHIFT 0\n"
                  "SHIFT -7\n"))))))

(deftest address-labels-and-tape-syntax
  (let [output (assembler/assemble-string
                 (str "; aliases can be forward referenced\n"
                      "==tape\n"
                      "#1\n"
                      "ADD SOURCE RESULT ; an inline comment\n"
                      "+12345678\n"
                      "00100\n"
                      "SOURCE = 10\n"
                      "RESULT = 20\n"))]
    (is (= ["==tape" "#1" "11020" "+12345678" "00100"]
           (significant-lines output)))
    (is (str/includes? output "; SOURCE = 10"))
    (is (str/includes? output "; an inline comment"))))

(deftest punched-tape-visualization
  (let [punched (assembler/punched-string
                  "==tape\n#1\n00100\n+12000000\n==tape\n#2\n00100\n")
        rows (str/split-lines punched)]
    (is (str/includes? punched "WITCH PUNCHED TAPE 1"))
    (is (str/includes? punched "WITCH PUNCHED TAPE 2"))
    (is (= 1 (count (re-seq #"· = blank; ● = punched; o = feed hole"
                            punched))))
    (is (some #{"  ● ● ·  o  ● ·   #1"} rows))
    (is (some #{"  · · ·  o  · ●   +"} rows))
    (is (some #{"  · · ●  o  ● ·   2"} rows))
    (is (= 16 (count (filter #(str/includes? % "  o  ")
                             (take-while #(not= "WITCH PUNCHED TAPE 2" %)
                                         rows)))))))

(deftest punched-code-table
  (is (= {\0 #{:a :b}, \1 #{:a :c}, \2 #{:b :c}, \3 #{:b :d},
          \4 #{:c :d}, \5 #{:c :e}, \6 #{:d :e}, \7 #{:a :d},
          \8 #{:a :e}, \9 #{:b :e}}
         assembler/digit-holes)))

(deftest assembler-output-options
  (is (= {:input "source.witch"
          :numerical-output "output.tape"
          :punched-output nil}
         (assembler/parse-cli-args ["source.witch" "output.tape"])))
  (is (= {:input "source.witch"
          :numerical-output nil
          :punched-output "output.punch"}
         (assembler/parse-cli-args
           ["source.witch" "--punch" "output.punch"])))
  (is (= {:input "source.witch"
          :numerical-output "output.tape"
          :punched-output "output.punch"}
         (assembler/parse-cli-args
           ["source.witch" "output.tape" "--punch" "output.punch"])))
  (is (= {:input "source.witch"
          :numerical-output nil
          :punched-output :stdout}
         (assembler/parse-cli-args ["source.witch" "--punch"])))
  (is (thrown? clojure.lang.ExceptionInfo
               (assembler/parse-cli-args ["source.witch"])))
  (is (thrown? clojure.lang.ExceptionInfo
               (assembler/parse-cli-args
                 ["source.witch" "same" "--punch" "same"]))))

(deftest writes-punched-output-to-stdout
  (let [source (java.io.File/createTempFile "witch-punch-" ".witch")]
    (try
      (spit source "==tape\n#1\nFINISH\n")
      (let [output (with-out-str
                     (assembler/-main (.getPath source) "--punch"))]
        (is (str/starts-with?
              output "· = blank; ● = punched; o = feed hole"))
        (is (str/includes? output "WITCH PUNCHED TAPE 1")))
      (finally
        (.delete source)))))

(deftest writes-punched-output
  (let [source (java.io.File/createTempFile "witch-punch-" ".witch")
        output (java.io.File/createTempFile "witch-punch-" ".txt")]
    (try
      (spit source "==tape\n#1\nFINISH\n")
      (assembler/assemble-punched-file (.getPath source) (.getPath output))
      (let [rendered (slurp output)]
        (is (str/includes? rendered "WITCH PUNCHED TAPE 1"))
        (is (str/includes? rendered "#1")))
      (finally
        (.delete source)
        (.delete output)))))

(deftest labels-are-case-insensitive
  (is (= ["11020"]
         (significant-lines
           (assembler/assemble-string
             "Source = 10\nRESULT = 20\nadd source result\n")))))

(deftest useful-assembler-errors
  (are [source message line-number]
       (try
         (assembler/assemble-string source)
         false
         (catch clojure.lang.ExceptionInfo e
           (and (re-find message (.getMessage e))
                (= line-number (:line-number (ex-data e))))))
    "WAT 10 20\n" #"Unknown order" 1
    "ADD MISSING 20\n" #"Unknown address" 1
    "ADD 10\n" #"expects 2 operands" 1
    "SIGNAL 3\n" #"Signal must" 1
    "SEARCH 10 2\n" #"Block number" 1
    "PRINT-LAYOUT 10\n" #"Print layout" 1
    "SHIFT -8\n" #"Shift must" 1
    "X = 10\nx = 20\n" #"Duplicate label" 2))

(deftest symbolic-programs-produce-valid-tapes
  (doseq [program ["bubble-sort" "counter" "demo-1" "demo-2"
                   "exercise-1" "exercise-2" "exercise-3" "exercise-4"
                   "exercise-5" "exercise-6" "factorial"
                   "fibonacci" "gcd" "indirect" "pascal" "pi"
                   "polynomial" "recursive-factorial" "sine"]]
    (let [assembled (java.io.File/createTempFile "witch-assembled-" ".tape")]
      (try
        (assembler/assemble-file (str "examples/" program ".witch")
                                 (.getPath assembled))
        (let [generated (atom nil)]
          (with-out-str
            (reset! generated (tape/read-tapes [(.getPath assembled)])))
          (is (seq @generated)
              (str program " should assemble to at least one tape")))
        (finally
          (.delete assembled))))))

(deftest raw-tape-example-matches-symbolic-source
  (let [assembled (java.io.File/createTempFile "witch-demo-" ".tape")]
    (try
      (assembler/assemble-file "examples/demo-1.witch" (.getPath assembled))
      (is (= (slurp "examples/demo-1.tape")
             (slurp assembled)))
      (finally
        (.delete assembled)))))

(deftest refuses-to-overwrite-source
  (let [source (java.io.File/createTempFile "witch-source-" ".witch")]
    (try
      (spit source "FINISH\n")
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"must be different"
                            (assembler/assemble-file (.getPath source)
                                                     (.getPath source))))
      (finally
        (.delete source)))))
