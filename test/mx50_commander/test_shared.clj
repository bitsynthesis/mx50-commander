(ns mx50-commander.test-shared
  (:require [mx50-commander.core :as core]))


(def cmds-sent (atom []))


(defn each-fixture [test-fn]
  (reset! cmds-sent [])
  (reset! core/devices {})
  (with-redefs [core/open-port (fn [_] :dummy-port)
                core/send-command (fn [_ cmd] (swap! cmds-sent conj cmd))
                core/device-default-rate 1]
    (test-fn)))
