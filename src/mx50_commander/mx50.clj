(ns mx50-commander.mx50)


(defn- pad-with-zeros
  [hex-str digits]
  (loop [s hex-str]
   (if (< (count s) digits)
     (recur (str "0" s))
     s)))


(defn- int-to-hex
  ([value] (int-to-hex value 2))
  ([value digits]
   (-> value
       java.lang.Integer/toHexString
       .toUpperCase
       (pad-with-zeros digits))))


(defn- restrict-range
  "Limit the range of an integer value. Defaults to 0 - 255."
  ([value] (restrict-range value 0))
  ([value minimum] (restrict-range value 0 255))
  ([value minimum maximum]
   (-> value
       (min maximum)
       (max minimum))))


(defn- default-hex-range
  "Converts integer to nearest 0 - 255 hex value."
  [value]
  (-> value restrict-range int-to-hex))


(defn- channel-a-b
  [channel]
  (case channel
    :a "A"
    :b "B"
    :both "T"))


(defn back-color
  "Background color.

   <color>  :bars :white :yellow :cyan :green :magenta :red :blue :black
   <gain>   0 - 255"
  [color gain]
  (format "VBC:%s%s"
          (case color
            :bars     "CB"
            :white    "WH"
            :yellow   "YL"
            :cyan     "CY"
            :green    "GR"
            :magenta  "MG"
            :red      "RD"
            :blue     "BU"
            :black    "BL")
          (default-hex-range gain)))


(defn color-correct
  "Color correct.

   <channel>  :a :b :both
   <red>      0 - 255
   <blue>     0 - 255"
  [channel red blue]
  (format "VCC:%s%s%s"
          (channel-a-b channel)
          (default-hex-range red)
          (default-hex-range blue)))


(defn color-correct-gain
  "Color correct gain.

   <channel>  :a :b :both
   <gain>     0 - 255"
  [channel gain]
  (format "VCG:%s%s"
          (channel-a-b channel)
          (default-hex-range gain)))


(defn wipe-position
  "Level of wipe lever.

   <level> 0 - 255"
  [level]
  (format "VWP:%s" (default-hex-range level)))
