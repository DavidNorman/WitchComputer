(ns witch.test-runner
  (:require [clojure.test :as test]
            [witch.additional-test]
            [witch.assembler-test]
            [witch.decode-test]
            [witch.machine-test]
            [witch.nines-test]
            [witch.tape-reader-test]
            [witch.tape-validation-test]
            [witch.utils-test]))

(def test-namespaces
  '[witch.additional-test
    witch.assembler-test
    witch.decode-test
    witch.machine-test
    witch.nines-test
    witch.tape-reader-test
    witch.tape-validation-test
    witch.utils-test])

(defn -main
  [& _]
  (let [{:keys [fail error]} (apply test/run-tests test-namespaces)]
    (when (pos? (+ fail error))
      (System/exit 1))))
