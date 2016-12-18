(ns mx50-commander.core-test
  (:require [clojure.test :refer :all]
            [mx50-commander.command :as cmd]
            [mx50-commander.core :as core]
            [mx50-commander.test-shared :as shared]))


(use-fixtures :each shared/each-fixture)


(deftest create-device
  (let [test-rate 200
        test-id :mx50-test
        _ (core/device test-id {:port "/dev/ttyUSB666" :rate test-rate})
        test-device (test-id @@#'core/devices)]
    (is (= :dummy-port (:port test-device)))
    (is (= test-rate (:rate test-device)))
    (is (= {} (:current test-device)))))


(deftest queue-commands
  (let [test-id :test-device
        test-cmd-1 "FOOBAR"
        test-cmd-2 "BARBAZ"
        test-device (core/device test-id)]
    (test-device test-cmd-1)
    (test-device test-cmd-2)
    (Thread/sleep 100)
    (is (= [test-cmd-1 test-cmd-2] @shared/cmds-sent))))


(deftest filter-duplicate-commands-by-cmd-id
  (let [test-id :test-device
        test-cmd-1 "VCG:TFF"
        test-cmd-2 "VMA:000"
        test-device (core/device test-id)]
    (dotimes [_ 10]
      (test-device test-cmd-1)
      (test-device test-cmd-2))
    (Thread/sleep 100)
    (is (= 2 (count @shared/cmds-sent)))))


(deftest no-filter-if-cache-disabled
  (let [test-id :test-device
        test-cmd-1 "VCG:TFF"
        test-device (core/device test-id)]
    (dotimes [_ 10]
      (test-device test-cmd-1 false))
    (Thread/sleep 100)
    (is (= 10 (count @shared/cmds-sent)))))


(deftest no-filter-changing-commands
  (let [test-id :test-device
        test-cmd-1 "VCG:TFF"
        test-cmd-2 "VCG:T00"
        test-device (core/device test-id)]
    (dotimes [_ 5]
      (test-device test-cmd-1)
      (test-device test-cmd-2))
    (Thread/sleep 100)
    (is (= 10 (count @shared/cmds-sent)))))


(deftest get-current
  (let [test-id :test-device
        test-cmd-1 "VCG:TFF"
        test-cmd-2 "VCG:T00"
        test-cmd-id :color-correct-gain
        test-device (core/device test-id)]
    (test-device test-cmd-1)
    (Thread/sleep 100)
    (is (= test-cmd-1 (core/get-current test-id test-cmd-id)))
    (test-device test-cmd-2)
    (Thread/sleep 100)
    (is (= test-cmd-2 (core/get-current test-id test-cmd-id)))))


(deftest start-stop-all-devices
  (let [test-device-1 (core/device :foo)
        test-device-2 (core/device :bar)]
    (core/stop)
    (test-device-1 "NOPE")
    (test-device-2 "NOPE")
    (core/start)
    (test-device-1 "YES-1A")
    (test-device-2 "YES-2A")
    (core/stop)
    (test-device-1 "NOPE")
    (test-device-2 "NOPE")
    (core/start)
    (test-device-1 "YES-1B")
    (test-device-2 "YES-2B")
    (Thread/sleep 100)
    (is (= (sort ["YES-1A" "YES-2A" "YES-1B" "YES-2B"])
           (sort @shared/cmds-sent)))))


(deftest start-stop-single-device
  (let [test-device-1 (core/device :foo)
        test-device-2 (core/device :bar)]
    (core/stop :bar)
    (test-device-1 "YES-1A")
    (test-device-2 "NOPE")
    (core/start :bar)
    (test-device-2 "YES-2A")
    (core/stop :foo)
    (test-device-1 "NOPE")
    (test-device-2 "YES-2B")
    (Thread/sleep 100)
    (is (= (sort ["YES-1A" "YES-2A" "YES-2B"])
           (sort @shared/cmds-sent)))))


(deftest clear-current-single-command
  (let [test-id-1 :test-device-1
        test-id-2 :test-device-2
        test-cmd-1 "VCG:T00"
        test-cmd-2 "VCC:TFFFF"
        test-cmd-id-1 :color-correct-gain
        test-cmd-id-2 :color-correct
        test-device-1 (core/device test-id-1)
        test-device-2 (core/device test-id-2)]
    (test-device-1 test-cmd-1 test-cmd-id-1)
    (test-device-1 test-cmd-2 test-cmd-id-2)
    (test-device-2 test-cmd-2 test-cmd-id-2)
    (Thread/sleep 100)
    (is (= test-cmd-1 (core/get-current test-id-1 test-cmd-id-1)))
    (core/clear-current test-id-1 test-cmd-id-2)
    (is (= test-cmd-1 (core/get-current test-id-1 test-cmd-id-1)))
    (is (= nil (core/get-current test-id-1 test-cmd-id-2)))
    (is (= test-cmd-2 (core/get-current test-id-2 test-cmd-id-2)))))


(deftest clear-current-single-device
  (let [test-id-1 :test-device-1
        test-id-2 :test-device-2
        test-cmd-1 "VCG:TFF"
        test-cmd-2 "VCC:T0000"
        test-cmd-id-1 :color-correct-gain
        test-cmd-id-2 :color-correct
        test-device-1 (core/device test-id-1)
        test-device-2 (core/device test-id-2)]
    (test-device-1 test-cmd-1)
    (test-device-1 test-cmd-2)
    (test-device-2 test-cmd-2)
    (Thread/sleep 100)
    (is (= test-cmd-1 (core/get-current test-id-1 test-cmd-id-1)))
    (core/clear-current test-id-1)
    (is (= nil (core/get-current test-id-1 test-cmd-id-1)))
    (is (= nil (core/get-current test-id-1 test-cmd-id-2)))
    (is (= test-cmd-2 (core/get-current test-id-2 test-cmd-id-2)))))


(deftest clear-current-all-devices
  (let [test-id-1 :test-device-1
        test-id-2 :test-device-2
        test-cmd-1 "VCG:T00"
        test-cmd-2 "VCG:TFF"
        test-cmd-id :color-correct-gain
        test-device-1 (core/device test-id-1)
        test-device-2 (core/device test-id-2)]
    (test-device-1 test-cmd-1)
    (test-device-2 test-cmd-2)
    (Thread/sleep 100)
    (is (= test-cmd-1 (core/get-current test-id-1 test-cmd-id)))
    (core/clear-current)
    (is (= nil (core/get-current test-id-1 test-cmd-id)))
    (is (= nil (core/get-current test-id-2 test-cmd-id)))))


(deftest rate-limit
  (let [test-id :test-device-1
        test-rate 100
        test-device (core/device test-id {:rate test-rate})
        num-cmds 10
        half-expected-time (/ (* test-rate num-cmds) 2)]
    (future
     (dotimes [_ num-cmds]
       (test-device "FOO" false)))
    (Thread/sleep half-expected-time)
    (is (<= (count @shared/cmds-sent) (/ num-cmds 2)))
    (Thread/sleep (+ 100 half-expected-time))
    (is (= num-cmds (count @shared/cmds-sent)))))


(deftest cache-key-lookup
  (let [counter (atom 0)
        test-cases [[(cmd/back-color 0 0 0)         :back-color]
                    [(cmd/back-color-preset :red 0) :back-color-preset]
                    [(cmd/color-correct :a 0 0 0)   :color-correct]
                    [(cmd/color-correct-gain :a 0)  :color-correct-gain]
                    [(cmd/color-correct-off :a)     :color-correct]
                    [(cmd/fade 000)                 :fade]
                    [(cmd/fade-level 0)             :fade-level]
                    [(cmd/fade-settings
                      :white false false false)     :fade-settings]
                    [(cmd/fx-mono :a true)          :fx-mono-a]
                    [(cmd/fx-mosaic :a 0)           :fx-mosaic-a]
                    [(cmd/fx-multi :a 1)            :fx-multi-a]
                    [(cmd/fx-negative :a true)      :fx-negative-a]
                    [(cmd/fx-strobe :b 0)           :fx-strobe-b]
                    [(cmd/input :a 0)               :input-a]
                    [(cmd/power)                    false]
                    [(cmd/wipe 0)                   :wipe]
                    [(cmd/wipe-border :none)        :wipe-border]
                    [(cmd/wipe-level 0)             :wipe-level]
                    [(cmd/wipe-one-way true)        :wipe-one-way]
                    [(cmd/wipe-pattern 0 0 :none)   :wipe-pattern]
                    [(cmd/wipe-reverse true)        :wipe-reverse]
                    ["FOOBAR"                       false]]]
    (doseq [[cmd cache-key] test-cases]
      (swap! counter inc)
      (is (= cache-key (#'core/get-cache-key cmd))))
    (is (< 0 @counter))
    (is (= (count test-cases) @counter))))
