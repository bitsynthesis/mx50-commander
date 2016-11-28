(ns mx-serial.core-test
  (:require [clojure.test :refer :all]
            [mx-serial.core :as mx]))


;; WARNING pseudo code - proposed API


(comment
 (def mx50
   (mx/device :mx50 {:port "/dev/ttyUSB0" :rate 100}))


 (defdevice mx50
   :port "/dev/ttyUSB0"
   :rate 100)


 (mx/clear-current)
 (mx/clear-current :mx50)
 (mx/clear-current :mx50 :wipe-position)


 (mx/start)
 (mx/start :mx50)


 (mx50 "PON")
 (mx50 (mx/wipe-position 255))
 (mx50 (mx/negative :a true))
 (mx50 (mx/mono :b false))
 (mx50 (mx/wipe-pattern 15 :reverse true))
 (mx50 "POF" :no-filter)


 (mx/get-current :mx50 :wipe-position)
 (mx/get-current :mx50 :negative-a)


 (mx/stop)
 (mx/stop :mx50))








(def cmds-sent (atom []))


(defn each-fixture [test-fn]
  (reset! cmds-sent [])
  (with-redefs [mx/open-port (fn [_] :dummy-port)
                mx/send-command (fn [_ cmd] (swap! cmds-sent conj cmd))]
    (test-fn)))


(use-fixtures :each each-fixture)


(deftest create-device
  (let [test-rate 200
        test-id :mx50-test
        _ (mx/device test-id {:port "/dev/ttyUSB666" :rate test-rate})
        test-device (test-id @mx/devices)]
    (is (= :dummy-port (:port test-device)))
    (is (= test-rate (:rate test-device)))
    (is (= {} (:current test-device)))))


;; TODO test that queue blocks
;; TODO rate (delay)
(deftest queue-commands
  (let [test-id :test-device
        test-cmd-1 "FOOBAR"
        test-cmd-2 "BARBAZ"
        test-device (mx/device test-id)]
    (mx/start test-id)
    (test-device test-cmd-1)
    (test-device test-cmd-2)
    (Thread/sleep 100)
    (is (= [test-cmd-1 test-cmd-2] @cmds-sent))))


(deftest filter-duplicate-commands-by-cmd-id
  (let [test-id :test-device
        test-cmd-1 "ZIGZAG"
        test-cmd-2 "ZARDOZ"
        test-cmd-id-1 :zig
        test-cmd-id-2 :connery
        test-device (mx/device test-id)]
    (mx/start test-id)
    (dotimes [_ 10]
      (test-device test-cmd-1 test-cmd-id-1)
      (test-device test-cmd-2 test-cmd-id-2))
    (Thread/sleep 100)
    (is (= 2 (count @cmds-sent)))))


(deftest no-filter-if-cache-disabled
  (let [test-id :test-device
        test-cmd-1 "ZIGZAG"
        test-device (mx/device test-id)]
    (mx/start test-id)
    (dotimes [_ 10]
      (test-device test-cmd-1 false))
    (Thread/sleep 100)
    (is (= 10 (count @cmds-sent)))))


(deftest no-filter-changing-commands
  (let [test-id :test-device
        test-cmd-1 "THIS"
        test-cmd-2 "THAT"
        test-cmd-id :zig
        test-device (mx/device test-id)]
    (mx/start test-id)
    (dotimes [_ 5]
      (test-device test-cmd-1 test-cmd-id)
      (test-device test-cmd-2 test-cmd-id))
    (Thread/sleep 100)
    (is (= 10 (count @cmds-sent)))))


(deftest get-current
  (let [test-id :test-device
        test-cmd-1 "THIS"
        test-cmd-2 "THAT"
        test-cmd-id :zig
        test-device (mx/device test-id)]
    (mx/start test-id)
    (test-device test-cmd-1 test-cmd-id)
    (Thread/sleep 100)
    (is (= test-cmd-1 (mx/get-current test-id test-cmd-id)))
    (test-device test-cmd-2 test-cmd-id)
    (Thread/sleep 100)
    (is (= test-cmd-2 (mx/get-current test-id test-cmd-id)))))


(deftest clear-current
  (let [test-id :test-device
        test-cmd-1 "THIS"
        test-cmd-2 "THAT"
        test-cmd-id :zig
        test-device (mx/device test-id)]
    (mx/start test-id)
    (test-device test-cmd-1 test-cmd-id)
    (is (= test-cmd-1 (mx/get-current test-id test-cmd-id)))
    (mx/clear-current test-id test-cmd-id)
    (is (= nil (mx/get-current test-id test-cmd-id)))))


(deftest start-stop-devices)
