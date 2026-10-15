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
    (when-let [unknown (first (remove #{"-s" "-t"} options))]
      (throw (ex-info (str "Unknown option: " unknown) {:option unknown})))
    (when-not (seq files)
      (throw (ex-info "A tape file is required" {})))
    {:files files
     :single-step (boolean (some #{"-s"} options))
     :trace (boolean (some #{"-t"} options))}))

(defn run-single-step
  "Run one order for each press of Return. Entering q leaves the machine paused."
  [machine-state]
  (loop [state machine-state]
    (if (or (:finished state) (:stopped state) (:waiting-signal state))
      state
      (do
        (pp/cl-format true
                      "[single step] PC ~a — press Return to execute, or q to quit: "
                      (:pc state))
        (flush)
        (let [command (read-line)]
          (cond
            (or (nil? command) (= "q" (.toLowerCase (.trim command))))
            (do
              (pp/cl-format true "~%Single-step execution paused.~%")
              state)

            (empty? (.trim command))
            (let [next-state (d/step state)]
              (m/dump-machine-state next-state)
              (recur next-state))

            :else
            (do
              (pp/cl-format true "Enter q to quit, or press Return to execute one order.~%")
              (recur state))))))))

(defn -main
  [& args]
  (try
    (let [{:keys [files single-step trace]} (parse-args args)]
      (pp/cl-format true "====Starting====~%")
      (let [initial-state (->
                            (assoc m/initial-machine-state
                              :tapes (tape/read-tapes files)
                              :trace trace)
                            (d/exec-search-tape 1 1)
                            (d/exec-transfer-control 21 1))
            final-state ((if single-step run-single-step d/run) initial-state)]
        (m/dump-machine-state final-state)))

    (catch ExceptionInfo e
      (pp/cl-format true "~a~%" (.getMessage e))
      (m/dump-machine-state (ex-data e)))
    (catch Exception e
      (pp/cl-format true "~a~%" (.getMessage e)))))
