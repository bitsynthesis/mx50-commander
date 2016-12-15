(ns mx50-commander.core
  (:require [clojure.core.async :refer [>!! <!!] :as a]))


(declare get-current)


(defrecord Command [id value])
(defrecord Device [consumer current port queue rate])


;; TODO make private
(def devices (atom {}))
(def ^:private device-default-rate 100)


(defn ^:private open-port [port]
  (let [baud-rate 9600
        data-bits 7
        stop-bits 1
        parity jssc.SerialPort/PARITY_ODD]
    (doto (jssc.SerialPort. port)
          .openPort
          (.setParams baud-rate data-bits stop-bits parity))))


(defn ^:private send-command
  [port cmd]
  (let [start-char (char 0x02)
        end-char (char 0x03)]
    (.writeBytes port (.getBytes (str start-char cmd end-char)))
    cmd))


(defn ^:private create-queue []
  (a/chan))


(defn ^:private get-queue [id]
  (:queue (id @devices)))


(defn ^:private queue-command
  ([device-id value] (queue-command device-id value false))
  ([device-id value cmd-id]
   (>!! (get-queue device-id)
        (map->Command {:id cmd-id :value value}))))


(defn ^:private create-consumer [id]
  (a/go
   (loop []
         (let [dev (id @devices)
               cmd (<!! (:queue dev))]
           (when (not (nil? cmd))
             (if (= false (:id cmd))
               (do
                 (send-command (:port dev) (:value cmd))
                 ;; TODO move this into send-command?
                 (Thread/sleep (:rate dev)))
               (when (not= (get-current id (:id cmd)) (:value cmd))
                 (swap! devices assoc-in [id :current (:id cmd)] (:value cmd))
                 (send-command (:port dev) (:value cmd))
                 ;; TODO move this into send-command?
                 (Thread/sleep (:rate dev))))
             (recur))))))


(defn get-current
  "Get the currently cached value of a command.

   |----------|----------------|
   |device-id | ex. :my-mixer  |
   |cmd-id    | ex. :back-color|"
  [device-id cmd-id]
  (cmd-id (:current (device-id @devices))))


(defn clear-current
  "Clear the cache for all devices, a single device, or a single command
   for a single device.

   |----------|----------------|
   |device-id | ex. :my-mixer  |
   |cmd-id    | ex. :back-color|"
  ([]
   (doseq [device-id (keys @devices)]
     (clear-current device-id)))
  ([device-id]
   (swap! devices assoc-in [device-id :current] {}))
  ([device-id cmd-id]
   (swap! devices assoc-in [device-id :current cmd-id] nil)))


;; TODO
;; - allow enabling / disabling caching for a device by default
;; - start new devices automatically when they are defined
(defn device
  "Register a device, returning a function to queue commands for execution.

   |-------|-----------------------------------|
   |id     | ex. :my-mixer                     |
   |params | ex. {:port \"/dev/ttyUSB0\" :rate 100}|"
  ([id] (device id {}))
  ([id params]
   (let [queue (create-queue)
         _ (a/close! queue) ;; start with a closed queue
         defaults {:rate device-default-rate}
         internals {:consumer nil
                    :current {}
                    :port (open-port (or (:port params) "/dev/ttyUSB0"))
                    :queue queue}
         definition (map->Device (merge defaults params internals))
         handler (partial queue-command id)]
     (swap! devices assoc id definition)
     handler)))


(defn stop
  "Stop sending commands to all devices or a single device.

   |---|--------------|
   |id | ex. :my-mixer|"
  ([]
   (doseq [id (keys @devices)]
     (stop id)))
  ([id]
   (let [old-queue (:queue (id @devices))]
     (a/close! old-queue))))


(defn start
  "Start sending commands to all devices or a single device.

   |---|--------------|
   |id | ex. :my-mixer|"
  ([]
   (doseq [id (keys @devices)]
     (start id)))
  ([id]
   (stop id)
   (swap! devices assoc-in [id :queue] (create-queue))
   (create-consumer id)))
