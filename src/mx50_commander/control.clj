(ns mx50-commander.control
  "Control MX devices, start and stop sending them commands, and access their
   last sent values."
  (:require [clojure.core.async :refer [>!! <!!] :as a]))


(declare get-current)


(defrecord Command [id value])
(defrecord Device [consumer current port port_ queue rate])


(def ^:private devices (atom {}))
(def ^:private device-defaults
  {:cache false
   :rate 100
   :port "/dev/ttyUSB0"})



(defn ^:private open-port [port]
  (let [baud-rate 9600
        data-bits 7
        stop-bits 1
        parity jssc.SerialPort/PARITY_ODD]
    (doto (jssc.SerialPort. port)
          .openPort
          (.setParams baud-rate data-bits stop-bits parity))))


(defn ^:private create-queue []
  (a/chan))


(def ^:private cache-keys
  {:back-color         "VB[MC]"
   :color-correct      "VCC"
   :color-correct-gain "VCG"
   :fade               "VFA"
   :fade-level         "VFM"
   :fade-settings      "VFD"
   :fx-mono-a          "VDE:AMN"
   :fx-mono-b          "VDE:BMN"
   :fx-mosaic-a        "VDE:AMS"
   :fx-mosaic-b        "VDE:BMS"
   :fx-multi-a         "VDM:A"
   :fx-multi-b         "VDM:B"
   :fx-negative-a      "VDE:ANG"
   :fx-negative-b      "VDE:BNG"
   :fx-strobe-a        "VDE:ASR"
   :fx-strobe-b        "VDE:BSR"
   :input-a            "VCP:A"
   :input-b            "VCP:B"
   :wipe               "VMA"
   :wipe-border        "VWB"
   :wipe-level         "VMM"
   :wipe-one-way       "VWD:X"
   :wipe-pattern       "VWP"
   :wipe-reverse       "VWD:[NF]X"})


(defn ^:private cache-key-lookup
  [cmd [k matcher]]
  (when (re-find (re-pattern (format "^%s" matcher)) cmd)
    k))


(defn ^:private get-cache-key
  [cmd]
  (or (some (partial cache-key-lookup cmd) cache-keys) false))


(defn ^:private queue-command
  ([device-id cmd] (queue-command device-id cmd (:cache (device-id @devices))))
  ([device-id cmd cache?]
   (let [cache-key (if cache? (get-cache-key cmd) false)]
     (>!! (:queue (device-id @devices))
          (map->Command {:id cache-key :value cmd})))))


(defn ^:private send-command
  [dev-id cmd-str]
  (let [port_ (:port_ (dev-id @devices))
        start-char (char 0x02)
        end-char (char 0x03)]
    (.writeBytes port_ (.getBytes (str start-char cmd-str end-char)))))


(defn ^:private send-cached-command
  [dev-id cmd]
  (let [ci (:id cmd)
        cv (:value cmd)]
    (if (false? ci)
      (send-command dev-id cv)
      (when (not= (get-current dev-id ci) cv)
        (swap! devices assoc-in [dev-id :current ci] cv)
        (send-command dev-id cv)))))


(defn ^:private create-consumer [dev-id]
  (a/go
   (loop []
     (let [dev (dev-id @devices)]
       (when-let [cmd (<!! (:queue dev))]
         (send-cached-command dev-id cmd)
         (Thread/sleep (:rate dev))
         (recur))))))


(defn get-current
  "|-----------|-----------------|
   | device-id | ex. :my-mixer   |
   | cmd-id    | ex. :back-color |

   Get the currently cached value of a command."
  [device-id cmd-id]
  (cmd-id (:current (device-id @devices))))


(defn clear-current
  "|-----------|-----------------|
   | device-id | ex. :my-mixer   |
   | cmd-id    | ex. :back-color |

   Clear the cached values for all devices, a single device, or a single command
   for a single device."
  ([]
   (doseq [device-id (keys @devices)]
     (clear-current device-id)))
  ([device-id]
   (swap! devices assoc-in [device-id :current] {}))
  ([device-id cmd-id]
   (swap! devices assoc-in [device-id :current cmd-id] nil)))


(defn stop
  "|-----------|---------------|
   | device-id | ex. :my-mixer |

   Stop sending commands to all devices or a single device."
  ([]
   (doseq [device-id (keys @devices)]
     (stop device-id)))
  ([device-id]
   (let [dev (device-id @devices)]
     (a/close! (:queue dev))
     (when-let [port_ (:port_ dev)]
       (.closePort port_)))))


(defn start
  "|-----------|---------------|
   | device-id | ex. :my-mixer |

   Start sending commands to all devices or a single device. If a device is already
   started, it will be stopped."
  ([]
   (doseq [device-id (keys @devices)]
     (start device-id)))
  ([device-id]
   (stop device-id)
   (let [port (:port (device-id @devices))]
     (swap! devices assoc-in [device-id :port_] (open-port port)))
   (swap! devices assoc-in [device-id :queue] (create-queue))
   (create-consumer device-id)))


(defn device
  "|--------|----------------------------------------|
   | id     | ex. :my-mixer                          |
   | params | ex. {:port \"/dev/ttyUSB0\" :rate 100} |

   Register a device, open a port, and start listening for commands. Return a
   function to queue commands for execution."
  ([id] (device id {}))
  ([id params]
   (when (id @devices)
     (stop id))
   (let [queue (create-queue)
         _ (a/close! queue) ;; start with a closed queue
         params-with-defaults (merge device-defaults params)
         internals {:consumer nil
                    :current {}
                    :port_ nil
                    :queue queue}
         definition (map->Device (merge params-with-defaults internals))
         handler (partial queue-command id)]
     (swap! devices assoc id definition)
     (start id)
     handler)))


;; TODO docs
(defn exit []
  (stop)
  (System/exit 0))
