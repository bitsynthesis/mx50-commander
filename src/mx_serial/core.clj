(ns mx-serial.core)


(defn create-port [params]
  (doto (jssc.SerialPort. (params :port-id))
        .openPort
        (.setParams (params :baud-rate)
                    (params :data-bits)
                    (params :stop-bits)
                    (params :parity))))


(defmacro defdevice
  [device-name & params]
  (let [defaults {:baud-rate 9600
                  :data-bits 7
                  :stop-bits 1
                  :parity jssc.SerialPort/PARITY_ODD
                  :port-id "/dev/ttyUSB0"
                  :start-char 0x02
                  :end-char 0x03}
        params-map (merge defaults (apply hash-map params))]
    `(def ~device-name ~(create-port params-map))))


(defn get-device-name [cmd-name]
  (re-find #"^[^-]+" cmd-name))


(defn send-command [device command]
  (.writeBytes device (.getBytes (str (char 0x02) command (char 0x03)))))


(defmacro defcommand
  [cmd-name arg-list & forms]
  ;; make this function execute the body with the arg list and pass it
  (let [device (-> cmd-name str get-device-name symbol)]
    (list 'defn cmd-name arg-list
          (list 'send-command device (cons 'do forms)))))


;; Usage


(defdevice mx50
  :port-id "/dev/ttyUSB1")


(defcommand mx50-neg [on-off]
  (if on-off
    "VDE:AMNN"
    "VDE:AMNF"))


(mx50-neg false)
(mx50-neg true)
