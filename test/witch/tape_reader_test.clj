(ns witch.tape-reader-test
  (:require [clojure.test :refer :all]
            [witch.tape-reader :as t]))

(deftest passing-tape
  (is (=
        (t/read-tapes ["test/resources/numerical-tape-pass"])
        [[[:no-block 0.0000M]
          [:no-block 1.1111M]
          [:no-block 2.2222M]
          [:no-block 3.3333M]
          [:no-block 4.4444M]
          [:no-block 5.5555M]
          [:no-block 6.6666M]
          [:no-block 7.7777M]
          [:no-block 8.8888M]
          [:no-block 9.9999M]
          [:no-block 0.1111M]
          [:no-block 1.2345678M]
          [:no-block -1.2345678M]
          [:no-block 0.1234567M]
          [:no-block -0.1234567M]
          [:block1 1.2345M]]
         [[:no-block 0.0000M]
          [:no-block 1.1111M]
          [:no-block 2.2222M]]])))


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
