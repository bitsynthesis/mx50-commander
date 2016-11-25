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


(deftest create-device
  (with-redefs [mx/open-port (fn [_] :dummy-port)]
    (let [test-rate 200
          test-id :mx50-test
          _ (mx/device test-id {:port "/dev/ttyUSB666" :rate test-rate})
          test-device (test-id @mx/devices)]
      (is (= :dummy-port (:port test-device)))
      (is (= test-rate (:rate test-device)))
      (is (= {} (:current test-device)))
      (is (= java.util.concurrent.ArrayBlockingQueue
             (class (:queue test-device)))))))


(deftest queue-commands
  (with-redefs [mx/open-port (fn [_] :dummy-port)]
    (let [test-id :test-device
          test-cmd-1 "FOOBAR"
          test-cmd-2 "BARBAZ"
          test-device (mx/device test-id)
          test-queue (:queue (test-id @mx/devices))
          test-done (atom false)]
      ;; test that queue blocks when not empty
      (future (do
                (test-device test-cmd-1)
                (test-device test-cmd-2)
                (reset! test-done true)))
      (is (= false @test-done))
      (is (= test-cmd-1 (.poll test-queue)))
      (Thread/sleep 100) ;; TODO handle more elegantly than sleeping
      (is (= true @test-done))
      (is (= test-cmd-2 (.poll test-queue)))
      (is (nil? (.poll test-queue))))))


(deftest send-commands)


(deftest filter-commands)


(deftest get-current)


(deftest clear-current)


(deftest start-stop-devices)
