(ns mx50-commander.midi-test
  (:require [clojure.test :refer :all]
            [midi :as m]
            [mx50-commander.midi :as mid]
            [mx50-commander.test-shared :as shared]))


(def midi-device-select (atom 0))
(def ch5 (atom []))
(def ch9 (atom []))


(defn midi-fixture [test-fn]
  (reset! midi-device-select 0)
  (reset! ch5 [])
  (reset! ch9 [])
  (reset! @#'mid/midi-buffers nil)
  (reset! @#'mid/midi-in nil)
  ;; mock midi-clj so test can call event handler directly
  (with-redefs [m/midi-in #(swap! midi-device-select inc)
                m/midi-handle-events (constantly nil)]
    (test-fn)))


(use-fixtures :each midi-fixture)


(deftest midi-triggers
  (mid/midi-start
   {;; quick enough to execute all
    5 (partial swap! ch5 conj)
    ;; slow enough to execute only first
    9 #(do (swap! ch9 conj %) (Thread/sleep 1000))})

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
  (is (= 1 (count @ch9))))


(deftest midi-device-prompt
  ;; devices already defined should only prompt device selection once
  (mid/midi-start {0 identity})
  (mid/midi-start {0 identity})
  (is (= 1 @midi-device-select))
  ;; unless re-selection is explicitly requested
  (mid/midi-start {0 identity} true)
  (is (= 2 @midi-device-select)))


(deftest midi-stop
  ;; TODO make sure stopping and restarting works
  )
