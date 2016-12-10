# Serial Port Selection

Use the following command to print recent serial port activity to determine what
port to set for your devices. Port names must be written in the form "/dev/ttyUSB0".

    dmesg | grep tty


# Proposed API

```
;; WARNING pseudo code

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
(mx/stop :mx50)
```
