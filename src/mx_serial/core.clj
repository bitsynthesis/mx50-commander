(ns mx-serial.core)


(defn- create-port [params]
  (doto (jssc.SerialPort. (params :port))
        .openPort
        (.setParams (params :baud-rate)
                    (params :data-bits)
                    (params :stop-bits)
                    (params :parity))))


(defn- get-device-name [cmd-name]
  (re-find #"^[^-]+" cmd-name))


(defn- send-command [device command]
  (.writeBytes device (.getBytes (str (char 0x02) command (char 0x03))))
  command)


(defn- pad-with-zeros
  [hex-str digits]
  (loop [s hex-str]
   (if (< (count s) digits)
     (recur (str "0" s))
     s)))


(defn int-to-hex
  ([value] (int-to-hex value 2))
  ([value digits]
   (-> value
       java.lang.Integer/toHexString
       .toUpperCase
       (pad-with-zeros digits))))


(defmacro defdevice
  "Define a port bound to its name."
  [device-name & params]
  (let [defaults {:baud-rate 9600
                  :data-bits 7
                  :stop-bits 1
                  :parity jssc.SerialPort/PARITY_ODD
                  :port "/dev/ttyUSB0"
                  :start-char 0x02
                  :end-char 0x03}
        params-map (merge defaults (apply hash-map params))]
    `(def ~device-name ~(create-port params-map))))


(defmacro defcommand
  "Define a function bound to its name which returns a command string which is
   sent to the device derived from the pre - part of the command name
   (ex. mx50 in mx50-neg). This may be overly clever,
   but it makes for a clean DSL."
  [cmd-name arg-list & forms]
  (let [device (-> cmd-name str get-device-name symbol)]
    (list 'defn cmd-name arg-list
          (list 'send-command device (cons 'do forms)))))
