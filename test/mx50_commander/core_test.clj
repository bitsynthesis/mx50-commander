(ns mx50-commander.core-test
  (:require [clojure.test :refer :all]
            [mx50-commander.test-shared :as shared]
            [mx50-commander.core :as core]))


(use-fixtures :each shared/each-fixture)


(deftest create-device
  (let [test-rate 200
        test-id :mx50-test
        _ (core/device test-id {:port "/dev/ttyUSB666" :rate test-rate})
        test-device (test-id @core/devices)]
    (is (= :dummy-port (:port test-device)))
    (is (= test-rate (:rate test-device)))
    (is (= {} (:current test-device)))))


(deftest queue-commands
  (let [test-id :test-device
        test-cmd-1 "FOOBAR"
        test-cmd-2 "BARBAZ"
        test-device (core/device test-id)]
    (core/start test-id)
    (test-device test-cmd-1)
    (test-device test-cmd-2)
    (Thread/sleep 100)
    (is (= [test-cmd-1 test-cmd-2] @shared/cmds-sent))))


(deftest filter-duplicate-commands-by-cmd-id
  (let [test-id :test-device
        test-cmd-1 "ZIGZAG"
        test-cmd-2 "ZARDOZ"
        test-cmd-id-1 :zig
        test-cmd-id-2 :connery
        test-device (core/device test-id)]
    (core/start test-id)
    (dotimes [_ 10]
      (test-device test-cmd-1 test-cmd-id-1)
      (test-device test-cmd-2 test-cmd-id-2))
    (Thread/sleep 100)
    (is (= 2 (count @shared/cmds-sent)))))


(deftest no-filter-if-cache-disabled
  (let [test-id :test-device
        test-cmd-1 "ZIGZAG"
        test-device (core/device test-id)]
    (core/start test-id)
    (dotimes [_ 10]
      (test-device test-cmd-1 false))
    (Thread/sleep 100)
    (is (= 10 (count @shared/cmds-sent)))))


(deftest no-filter-changing-commands
  (let [test-id :test-device
        test-cmd-1 "THIS"
        test-cmd-2 "THAT"
        test-cmd-id :zig
        test-device (core/device test-id)]
    (core/start test-id)
    (dotimes [_ 5]
      (test-device test-cmd-1 test-cmd-id)
      (test-device test-cmd-2 test-cmd-id))
    (Thread/sleep 100)
    (is (= 10 (count @shared/cmds-sent)))))


(deftest get-current
  (let [test-id :test-device
        test-cmd-1 "THIS"
        test-cmd-2 "THAT"
        test-cmd-id :zig
        test-device (core/device test-id)]
    (core/start test-id)
    (test-device test-cmd-1 test-cmd-id)
    (Thread/sleep 100)
    (is (= test-cmd-1 (core/get-current test-id test-cmd-id)))
    (test-device test-cmd-2 test-cmd-id)
    (Thread/sleep 100)
    (is (= test-cmd-2 (core/get-current test-id test-cmd-id)))))


(deftest start-stop-all-devices
  (let [test-device-1 (core/device :foo)
        test-device-2 (core/device :bar)]
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
    (core/start :foo)
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
        test-cmd-1 "THIS"
        test-cmd-2 "THAT"
        test-cmd-id-1 :zig
        test-cmd-id-2 :zag
        test-device-1 (core/device test-id-1)
        test-device-2 (core/device test-id-2)]
    (core/start)
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
        test-cmd-1 "THIS"
        test-cmd-2 "THAT"
        test-cmd-id-1 :zig
        test-cmd-id-2 :zag
        test-device-1 (core/device test-id-1)
        test-device-2 (core/device test-id-2)]
    (core/start)
    (test-device-1 test-cmd-1 test-cmd-id-1)
    (test-device-1 test-cmd-2 test-cmd-id-2)
    (test-device-2 test-cmd-2 test-cmd-id-2)
    (Thread/sleep 100)
    (is (= test-cmd-1 (core/get-current test-id-1 test-cmd-id-1)))
    (core/clear-current test-id-1)
    (is (= nil (core/get-current test-id-1 test-cmd-id-1)))
    (is (= nil (core/get-current test-id-1 test-cmd-id-2)))
    (is (= test-cmd-2 (core/get-current test-id-2 test-cmd-id-2)))))


(deftest clear-current-all-devices
  (let [test-id-1 :test-device-1
        test-id-2 :test-device-2
        test-cmd-1 "THIS"
        test-cmd-2 "THAT"
        test-cmd-id :zig
        test-device-1 (core/device test-id-1)
        test-device-2 (core/device test-id-2)]
    (core/start test-id-1)
    (test-device-1 test-cmd-1 test-cmd-id)
    (test-device-2 test-cmd-2 test-cmd-id)
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
    (core/start)
    (future
     (dotimes [_ num-cmds]
       (test-device "FOO" false)))
    (Thread/sleep half-expected-time)
    (is (<= (count @shared/cmds-sent) (/ num-cmds 2)))
    (Thread/sleep (+ 100 half-expected-time))
    (is (= num-cmds (count @shared/cmds-sent)))))


;; TODO
;; - port opening / closing
