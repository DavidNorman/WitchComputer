(ns witch.tape-reader-test
  (:require [clojure.test :refer :all]
            [witch.tape-reader :as t]))

(deftest passing-tape
  (is (=
        (t/read-tapes ["test/resources/numerical-tape-pass"])
        [[0.0000M
          1.1111M
          2.2222M
          3.3333M
          4.4444M
          5.5555M
          6.6666M
          7.7777M
          8.8888M
          9.9999M
          0.1111M
          1.2345678M
          -1.2345678M
          0.1234567M
          -0.1234567M
          :block1
          1.2345M]
         [0.0000M
          1.1111M
          2.2222M]])))


(deftest failing-tape-1
  (is (thrown? Exception (t/read-tapes ["test/resources/numerical-tape-fail"]))))

(deftest failing-tape-2
  (is (thrown? Exception (t/read-tapes ["test/resources/numerical-tape-fail-2"]))))

(deftest failing-tape-3
  (is (thrown? Exception (t/read-tapes ["test/resources/numerical-tape-fail-3"]))))

(deftest non-existent-tape
  (is (thrown? java.io.FileNotFoundException (t/read-tapes ["__no-tape__"]))))

(deftest nil-tape
  (is (= (t/read-tapes nil) nil)))
