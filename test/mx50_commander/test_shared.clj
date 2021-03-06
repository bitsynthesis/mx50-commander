(ns mx50-commander.test-shared
  (:require [mx50-commander.control :as con]))


(def cmds-sent (atom []))


(defn each-fixture [test-fn]
  (reset! cmds-sent [])
  (reset! @#'con/devices {})
  (with-redefs [con/open-port (fn [_] nil)
                con/select-port (fn [_] "/dev/ttyUSB0")
                con/send-command (fn [_ cmd] (swap! cmds-sent conj cmd))
                con/device-defaults {:cache true :rate 0}]
    (test-fn)))
