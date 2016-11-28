# Serial Port Selection

Use the following command to print recent serial port activity to determine what
port to set for your devices. Port names must be written in the form "/dev/ttyUSB0".

    dmesg | grep tty
