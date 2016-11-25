(ns mx-serial.core
  (:require [clojure.core.async :refer [>! <! >!! <!!] :as a]))
;;
;;
;; (defn- get-device-name [cmd-name]
;;   (re-find #"^[^-]+" cmd-name))
;;
;;
;; (defn- pad-with-zeros
;;   [hex-str digits]
;;   (loop [s hex-str]
;;    (if (< (count s) digits)
;;      (recur (str "0" s))
;;      s)))
;;
;;
;; (defn int-to-hex
;;   ([value] (int-to-hex value 2))
;;   ([value digits]
;;    (-> value
;;        java.lang.Integer/toHexString
;;        .toUpperCase
;;        (pad-with-zeros digits))))
;;
;;
;; (defmacro defdevice
;;   "Define a port bound to its name."
;;   [device-name & params]
;;   (let [defaults {:baud-rate 9600
;;                   :data-bits 7
;;                   :stop-bits 1
;;                   :parity jssc.SerialPort/PARITY_ODD
;;                   :port "/dev/ttyUSB0"
;;                   :start-char 0x02
;;                   :end-char 0x03}
;;         params-map (merge defaults (apply hash-map params))]
;;     `(def ~device-name ~(open-port params-map))))
;;
;;
;; (defmacro defcommand
;;   "Define a function bound to its name which returns a command string which is
;;    sent to the device derived from the pre - part of the command name
;;    (ex. mx50 in mx50-neg). This may be overly clever,
;;    but it makes for a clean DSL."
;;   [cmd-name arg-list & forms]
;;   (let [device (-> cmd-name str get-device-name symbol)]
;;     (list 'defn cmd-name arg-list
;;           (list command device (cons 'do forms)))))
;;
















(defrecord Device [current port queue rate])


(def devices (atom {}))


(defn- open-port [port]
  (let [baud-rate 9600
        data-bits 7
        stop-bits 1
        parity jssc.SerialPort/PARITY_ODD]
    (doto (jssc.SerialPort. port)
          .openPort
          (.setParams baud-rate data-bits stop-bits parity))))


(defn- send-command
  "Send command string to device."
  [device cmd]
  (let [start-char 0x02
        end-char 0x03]
    (when cmd
      (.writeBytes device (.getBytes (str start-char cmd end-char))))
    cmd))


(defn device
  "Register a device, returning a function to queue commands for execution."
  ([id] (device id {}))
  ([id params]
   (let [defaults {:rate 100}
         internals {:current {}
                    :port (open-port (or (:port params) "/dev/ttyUSB0"))
                    :queue (java.util.concurrent.ArrayBlockingQueue. 1)}
         definition (map->Device (merge defaults params internals))
         handler (fn [cmd] (.put (:queue definition) cmd))]
     (swap! devices assoc id definition)
     handler)))
