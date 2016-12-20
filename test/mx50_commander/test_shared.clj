(ns mx50-commander.test-shared
  (:require [mx50-commander.core :as core]))


(def cmds-sent (atom []))


(defn each-fixture [test-fn]
  (reset! cmds-sent [])
  (reset! @#'core/devices {})
  (with-redefs [core/open-port (fn [_] nil)
                core/send-command (fn [_ cmd] (swap! cmds-sent conj cmd))
                core/device-defaults {:cache true
                                      :rate 0
                                      :port "/dev/ttyUSB0"}]
    (test-fn)))
