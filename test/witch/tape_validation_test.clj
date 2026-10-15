(ns witch.tape-validation-test
  (:require [clojure.test :refer :all]
            [witch.tape-reader :as tape]))

(defn with-tape-file
  [content f]
  (let [file (java.io.File/createTempFile "witch-tape-" ".txt")]
    (try
      (spit file content)
      (f (.getPath file))
      (finally
        (.delete file)))))

(deftest tape-file-structure
  (with-tape-file "12345\n"
    #(is (thrown-with-msg? clojure.lang.ExceptionInfo
                           #"must begin"
                           (tape/read-tapes [%]))))
  (with-tape-file ""
    #(is (thrown-with-msg? clojure.lang.ExceptionInfo
                           #"must begin"
                           (tape/read-tapes [%]))))
  (with-tape-file "==tape\n"
    #(is (thrown-with-msg? clojure.lang.ExceptionInfo
                           #"at least one value"
                           (tape/read-tapes [%]))))
  (with-tape-file "==tape\n==tape\n12345\n"
    #(is (thrown-with-msg? clojure.lang.ExceptionInfo
                           #"at least one value"
                           (tape/read-tapes [%]))))
  (with-tape-file "; comment\n\n==tape\n#1\n12345\n\n; end\n"
    #(is (= [[[:block1 1.2345M]]] (tape/read-tapes [%]))))
  (with-tape-file "==tape\n12345\n==tape\n#2\n+12345678\n"
    #(is (= [[[:no-block 1.2345M]] [[:block2 1.2345678M]]]
            (tape/read-tapes [%])))))

(deftest malformed-values-have-context
  (doseq [bad ["#10" "#x" "1234" "123456" "+1234567" "+123456789"
               " 12345" "12345 "]]
    (with-tape-file (str "==tape\n" bad "\n")
      (fn [filename]
        (try
          (tape/read-tapes [filename])
          (is false (str "Expected failure for " bad))
          (catch clojure.lang.ExceptionInfo e
            (is (= filename (:filename (ex-data e))))
            (is (= 2 (:line (ex-data e))))))))))

(deftest multiple-input-files
  (with-tape-file "==tape\n12345\n"
    (fn [first-file]
      (with-tape-file "==tape\n#2\n+12345678\n"
        (fn [second-file]
          (is (= [[[:no-block 1.2345M]] [[:block2 1.2345678M]]]
                 (tape/read-tapes [first-file second-file]))))))))
