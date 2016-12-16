(ns mx50-commander.convert
  "Helper functions for converting between color and number formats.
   These helpers are primarily utilized by the functions in `mx50-commander.command`,
   but are made available for projects which require more granular control."
  (:import java.lang.Math))


(defn pad-with-zeros
  ;; TODO docs
  ([value] (pad-with-zeros value 2))
  ([value digits]
   (loop [s (str value)]
     (if (< (count s) digits)
       (recur (str "0" s))
       s))))


(defn int-to-hex
  ;; TODO docs
  ([value] (int-to-hex value 2))
  ([value digits]
   (-> value
       java.lang.Integer/toHexString
       .toUpperCase
       (pad-with-zeros digits))))


(defn restrict-range
  ;; TODO docs
  "Limit the range of an integer value."
  [value minimum maximum]
  (-> value
      (min maximum)
      (max minimum)))


(defn default-hex-range
  ;; TODO docs
  "Converts integer to nearest 0 - 255 hex value."
  [value]
  (-> value (restrict-range 0 255) int-to-hex))


(defn channel-a-b
  ;; TODO docs
  [channel]
  (case channel
    :a "A"
    :b "B"
    :both "T"))


(defn rgb-to-ypbpr
  ;; TODO docs
  "Returns [y p-b p-r]"
  [r g b]
  (mapv int
        [(+ (* r 0.257) (* g 0.504) (* b 0.098) 16)
         (+ (* r 0.439) (* g -0.368) (* b -0.071) 128)
         (+ (* r -0.148) (* g -0.291) (* b 0.439) 128)]))


(defn ^:private in-range
  [[minimum maximum] value]
  (and (<= minimum value)
       (< value maximum)))


(defn hsl-to-rgb
  ;; TODO docs
  "|---|---------|-----------------------|
   | h | 0 - 360 | hue in degrees        |
   | s | 0 - 100 | saturation in percent |
   | l | 0 - 100 | lightness in percent  |

   http://www.rapidtables.com/convert/color/hsl-to-rgb.htm
   "
  [h s l]
  (let [h_ (restrict-range h 0 360)
        s_ (/ (restrict-range s 0 100) 100)
        l_ (/ (restrict-range l 0 100) 100)
        c (* s_ (- 1 (Math/abs (float (- (* 2 l_) 1)))))
        x (* c (- 1 (Math/abs (float (- (mod (/ h_ 60) 2)  1)))))
        m (- l_ (/ c 2))
        rgb_ (condp in-range h_
                    [0 60]    [c x 0]
                    [60 120]  [x c 0]
                    [120 180] [0 c x]
                    [180 240] [0 x c]
                    [240 300] [x 0 c]
                    [300 360] [c 0 x])]
    (mapv #(-> % (+ m) (* 255) Math/ceil int)
          rgb_)))
