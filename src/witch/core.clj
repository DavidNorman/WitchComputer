(ns witch.core
  (:require [witch.tape-reader :as tape]
            [witch.machine :as m]
            [witch.decode :as d]
            [clojure.pprint :as pp])
  (:gen-class)
  (:import (clojure.lang ExceptionInfo)))

(defn -main
  [& args]
  (try
    (pp/cl-format true "====Starting====~%")

    (->
      (assoc m/initial-machine-state
        :tapes (tape/read-tapes args)
        :trace false)
      (d/exec-search-tape 1 1)
      (d/exec-transfer-control 21 1)
      (d/run)
      (m/dump-machine-state))

    (catch ExceptionInfo e
      (pp/cl-format true "~a~%" (.getMessage e))
      (m/dump-machine-state (ex-data e)))
    (catch Exception e
      (pp/cl-format true "~a~%" (.getMessage e)))))