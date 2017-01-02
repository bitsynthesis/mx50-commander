(ns mx50-commander.midi-test
  (:require [clojure.test :refer :all]
            [midi :as m]
            [mx50-commander.midi :as mid]
            [mx50-commander.test-shared :as shared]))


(deftest midi-basics
  (let [ch5 (atom [])
        ch9 (atom [])]
    ;; mock midi-clj so test can call event handler directly
    (with-redefs [m/midi-in (constantly :dummy)
                  m/midi-handle-events (constantly nil)]

      (mid/midi-start
       ;; quick enough to execute all
       5 (partial swap! ch5 conj)
       ;; slow enough to execute only first
       9 #(do (swap! ch9 conj %) (Thread/sleep 1000)))

      (doseq [chan [0
                    1
                    2 2
                    5 5 5 5 5         ;; 5 fives
                    6 6 6 6 6 6
                    9 9 9 9 9 9 9 9 9 ;; 9 nines
                    15]]
        (#'mid/midi-handler {:chan chan} 12345)
        ;; prevent test races by giving consumer time to process each note
        (Thread/sleep 10))

      ;; all
      (is (= 5 (count @ch5)))
      ;; only first
      (is (= 1 (count @ch9))))))
