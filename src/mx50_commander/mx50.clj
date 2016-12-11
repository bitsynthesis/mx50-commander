(ns mx50-commander.mx50)


(defn- pad-with-zeros
  [value digits]
  (loop [s (str value)]
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
  ([value minimum] (restrict-range value minimum 255))
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


(defn color-correct-off
  "Turn color correct off."
  [channel]
  (format "VCC:%sOF" (channel-a-b channel)))


(defn color-correct-gain
  "Color correct gain.

   <channel>  :a :b :both
   <gain>     0 - 255"
  [channel gain]
  (format "VCG:%s%s"
          (channel-a-b channel)
          (default-hex-range gain)))


(defn negative
  "Negative effect.

   <channel>  :a :b
   <on-off>   true false"
  [channel on-off]
  (format "VDE:%sNG%s"
          (channel-a-b channel)
          (if on-off "NN" "FF")))


(defn mosaic
  "Mosaic effect.

   <channel>  :a :b
   <size>     0 - 30"
  [channel size]
  (let [value (if size
                (-> size (restrict-range 0 30) int-to-hex)
                "OF")]
    (format "VDE:%sMS%s"
            (channel-a-b channel)
            value)))


(defn mono
  "Mono effect.

   <channel>  :a :b
   <on-off>   true false"
  [channel on-off]
  (format "VDE:%sMN%s" (channel-a-b channel) (if on-off "N" "F")))


(defn strobe
  "Strobe effect.

   <channel>  :a :b
   <slowness> 0 - 62"
  [channel slowness]
  (format "VDE:%sSR%s"
          (channel-a-b channel)
          (if slowness
            (-> slowness (restrict-range 0 62) int-to-hex)
            "OF")))


(defn auto-fade
  "Execute fade at fixed rate.

   <frames> 0 - 999"
  [frames]
  (format "VFA:%s" (-> frames (restrict-range 0 999) (pad-with-zeros 3))))


(defn source-select
  "Select video input.

   <channel>  :a :b
   <input>    1 - 5"
  [channel input]
  (format "VCP:%s%s"
          (channel-a-b channel)
          (restrict-range input 1 5)))


(defn mix-level
  "Level of mix lever.

   <level> 0 - 255"
  [level]
  (format "VMM:%s" (default-hex-range level)))
