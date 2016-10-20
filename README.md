# Serial Port Selection

Use the following command to print recent serial port activity to determine what
port to set for your devices. Port names must be written in the form "/dev/ttyUSB0".

    dmesg | grep tty


# Useage

    (defdevice mx50
      :port "/dev/ttyUSB1")


    (defcommand mx50-neg [on-off]
      (if on-off
        "VDE:AMNN"
        "VDE:AMNF"))


    (defcommand mx50-fade [pct]
      (str "VFM:"
           (-> pct
               (/ 100)
               (* 256)
               (min 255)
               int-to-hex)))


    (mx50-neg false)
    (mx50-neg true)


    (mx50-fade 0)
    (mx50-fade 50)
    (mx50-fade 100)

    (mx50-fade (rand-int 100))
