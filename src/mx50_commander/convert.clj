(ns mx50-commander.convert
  "Convert between color and number formats."
  (:import java.lang.Math))


(defn zero-pad
  "|--------|-------------------|------------------------------------|
   | value  | string or integer | value to pad                       |
   | digits | positive integer  | minimum number of digits to pad to |

   Convert a value to a zero padded string with a fixed number of digits."
  ([value] (zero-pad value 2))
  ([value digits]
   (loop [s (str value)]
     (if (< (count s) digits)
       (recur (str "0" s))
       s))))


(defn int-to-hex
  "Convert an integer to a hex string of a fixed number of digits."
  ([value] (int-to-hex value 2))
  ([value digits]
   (-> value
       java.lang.Integer/toHexString
       .toUpperCase
       (zero-pad digits))))


(defn restrict-range
  "Limit the range of an integer value."
  [value minimum maximum]
  (-> value
      (min maximum)
      (max minimum)))


(defn default-hex-range
  "Convert an integer to nearest two digit 00 - FF hex string."
  [value]
  (-> value (restrict-range 0 255) int-to-hex))


(defn rgb-to-ypbpr
  "|---|---------|---------------------|
   | r | 0 - 255 | red channel value   |
   | g | 0 - 255 | green channel value |
   | b | 0 - 255 | blue channel value  |

   Convert RGB color to YPbPr."
  [r g b]
  (mapv int
        [(+ (* r 0.257) (* g 0.504) (* b 0.098) 16)
         (+ (* r -0.148) (* g -0.291) (* b 0.439) 128)
         (+ (* r 0.439) (* g -0.368) (* b -0.071) 128)]))


(defn ^:private in-range
  [[minimum maximum] value]
  (and (<= minimum value)
       (< value maximum)))


(defn hsl-to-rgb
  "|---|---------|-----------------------|
   | h | 0 - 360 | hue in degrees        |
   | s | 0 - 100 | saturation in percent |
   | l | 0 - 100 | lightness in percent  |

   http://www.rapidtables.com/convert/color/hsl-to-rgb.htm

   Convert HSL color to RGB."
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


;; TODO doc
(defn relative
  "Get the item in a collection that corresponds with the value relative to
   the range 0 - maximum."
  [coll maximum value]
  (let [index (-> value
                  (max 0)
                  (/ maximum)
                  (* (count coll))
                  int
                  (min (- (count coll) 1)))]
    (nth coll index)))
