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
















(defrecord Device [consumer current port queue rate])


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


(defn- create-queue []
  (a/chan))


(defn get-queue [id]
  (:queue (id @devices)))


(defrecord Command [id value])


(defn queue-command
  ([device-id value] (queue-command device-id value false))
  ([device-id value cmd-id]
   ;; TODO set the cache. send-command is also a logical place, but that would
   ;; require passing a more complex data structure through the queue to
   ;; include the cache-id
   (>!! (get-queue device-id)
        (map->Command {:id cmd-id :value value}))))


(defn get-current [id cmd-id]
  (cmd-id (:current (id @devices))))


(defn clear-current
  ([])
  ([id])
  ([id cmd-id]
   (swap! devices assoc-in [id :current cmd-id] nil)))


(defn device
  "Register a device, returning a function to queue commands for execution."
  ([id] (device id {}))
  ([id params]
   (let [queue (create-queue)
         _ (a/close! queue) ;; start with a closed queue
         defaults {:rate 100}
         internals {:consumer nil
                    :current {}
                    :port (open-port (or (:port params) "/dev/ttyUSB0"))
                    :queue queue}
         definition (map->Device (merge defaults params internals))
         handler (partial queue-command id)]
     (swap! devices assoc id definition)
     handler)))


;; TODO test
(defn stop
  ([])
  ([id]
   (let [old-queue (:queue (id @devices))]
     (a/close! old-queue))))


;; TODO test
(defn start
  ([])
  ([id]
   ;; make sure an existing channel is stopped
   (stop id)
   ;; create a new channel
   (swap! devices assoc-in [id :queue] (create-queue))
   ;; create the consumer
   (a/go
    (loop []
          (let [dev (id @devices)
                cmd (<!! (:queue dev))]
            (when (not (nil? cmd))
              (if (= false (:id cmd))
                (send-command (:port dev) (:value cmd))
                (when (not= (get-current id (:id cmd)) (:value cmd))
                  (swap! devices assoc-in [id :current (:id cmd)] (:value cmd))
                  (send-command (:port dev) (:value cmd))))
              (recur)))))))
