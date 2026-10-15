(ns witch.core
  (:require [witch.tape-reader :as tape]
            [witch.machine :as m]
            [witch.decode :as d]
            [clojure.pprint :as pp])
  (:gen-class)
  (:import (clojure.lang ExceptionInfo)))

(defn doesn't-start-with-hyphen
  [str]
  (not (.startsWith str "-")))

(defn parse-args
  [args]
  (let [options (filter #(.startsWith % "-") args)
        files (filter doesn't-start-with-hyphen args)]
    (when-let [unknown (first (remove #{"-t"} options))]
      (throw (ex-info (str "Unknown option: " unknown) {:option unknown})))
    (when-not (seq files)
      (throw (ex-info "A tape file is required" {})))
    {:files files
     :trace (boolean (some #{"-t"} options))}))

(defn -main
  [& args]
  (try
    (let [{:keys [files trace]} (parse-args args)]

      (pp/cl-format true "====Starting====~%")
      (->
        (assoc m/initial-machine-state
          :tapes (tape/read-tapes files)
          :trace trace)
        (d/exec-search-tape 1 1)
        (d/exec-transfer-control 21 1)
        (d/run)
        (m/dump-machine-state)))

    (catch ExceptionInfo e
      (pp/cl-format true "~a~%" (.getMessage e))
      (m/dump-machine-state (ex-data e)))
    (catch Exception e
      (pp/cl-format true "~a~%" (.getMessage e)))))
