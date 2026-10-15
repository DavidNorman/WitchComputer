(ns witch.tape-reader
  (:require [clojure.pprint :as pp]
            [witch.nines :as n]))

(def block-markers
  {"#0" :block0
   "#1" :block1
   "#2" :block2
   "#3" :block3
   "#4" :block4
   "#5" :block5
   "#6" :block6
   "#7" :block7
   "#8" :block8
   "#9" :block9
   })

(defn convert-tape-string
  [l]
  (let [m1 (re-find #"^[-+]\d{8}$" l)
        m2 (re-find #"^\d{5}+$" l)
        m3 (re-find #"#\d$" l)]

    (when-not (or m1 m2 m3)
      (throw (Exception. (str "Numerical tape format error:" l))))

    (cond
      m3 (get block-markers m3)
      m2 (n/to-nines (/ (bigdec m2) 10000M))
      m1 (n/to-nines (/ (bigdec m1) 10000000M)))))

(defn process-one-tape
  [s]
  (doall
    (->>
      s
      (map convert-tape-string)
      (concat [:no-block])
      (partition 2 1)
      (remove (comp keyword? second))
      (map
        (fn [[a b]] (if (number? a) [:no-block b] [a b]))))))

(defn comment-or-blank?
  [l]
  (re-matches #"^;.*|\s*" l))

(defn read-tapes
  [args]
  (when (seq args)
    (into []
          (mapcat
            (fn [filename]
              (pp/cl-format true "Loading tapes ~a~%" filename)
              (with-open [reader (clojure.java.io/reader filename)]
                (let [lines (doall
                              (->> (line-seq reader)
                                   (map-indexed (fn [index text]
                                                  {:line (inc index) :text text}))
                                   (remove #(comment-or-blank? (:text %)))))]
                  (when-not (= "==tape" (:text (first lines)))
                    (throw (ex-info "Tape file must begin with ==tape"
                                    {:filename filename
                                     :line (:line (first lines))})))
                  (when-let [empty-marker
                             (first
                               (for [[a b] (partition 2 1 lines)
                                     :when (and (= "==tape" (:text a))
                                                (= "==tape" (:text b)))]
                                 b))]
                    (throw (ex-info "Tape must contain at least one value"
                                    {:filename filename
                                     :line (:line empty-marker)})))
                  (when (= "==tape" (:text (last lines)))
                    (throw (ex-info "Tape must contain at least one value"
                                    {:filename filename
                                     :line (:line (last lines))})))
                  (let [groups (->> lines
                                    (partition-by #(= "==tape" (:text %)))
                                    (remove #(= "==tape" (:text (first %)))))]
                    (when (some empty? groups)
                      (throw (ex-info "Tape must contain at least one value"
                                      {:filename filename})))
                    (map
                      (fn [group]
                        (try
                          (process-one-tape (map :text group))
                          (catch Exception e
                            (let [bad-line (first
                                             (filter
                                               (fn [{:keys [text]}]
                                                 (try
                                                   (convert-tape-string text)
                                                   false
                                                   (catch Exception _ true)))
                                               group))]
                              (throw (ex-info (.getMessage e)
                                              {:filename filename
                                               :line (:line bad-line)}
                                              e))))))
                      groups)))))
            args))))
