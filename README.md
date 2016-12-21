# MX50 Commander

A Clojure library for controlling Panasonic WJ-MX50 and WJ-MX30 video mixers via USB RS232 serial port adapter.

*This library is in pre-alpha. It is not completely documented, and the API is unstable.*

# Serial Port Selection

Use the following command to print recent serial port activity to determine what
port to set for your devices. Port names must be written in the form "/dev/ttyUSB0".

    dmesg | grep tty
