(ns mx50-commander.examples.basic
  (:require [mx50-commander.command :as cmd]
            [mx50-commander.control :as con]
            [mx50-commander.generate :as gen]))


(def mixer
  (con/device :mixer {:port "/dev/ttyUSB1"}))

(mixer (cmd/wipe-level 0))
(mixer (cmd/input :a 0))
(mixer (cmd/back-color-preset :red 127))
(mixer (cmd/wipe-pattern 6 3))

(gen/generate [s (range 20)
               x (range 120)]
  (mixer (cmd/wipe-level x))
  (mixer (cmd/fx-negative :b (even? s))))
