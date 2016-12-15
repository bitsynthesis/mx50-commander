(ns mx50-commander.mx50)


(defn ^:private pad-with-zeros
  [value digits]
  (loop [s (str value)]
   (if (< (count s) digits)
     (recur (str "0" s))
     s)))


(defn ^:private int-to-hex
  ([value] (int-to-hex value 2))
  ([value digits]
   (-> value
       java.lang.Integer/toHexString
       .toUpperCase
       (pad-with-zeros digits))))


(defn ^:private restrict-range
  "Limit the range of an integer value. Defaults to 0 - 255."
  ([value] (restrict-range value 0))
  ([value minimum] (restrict-range value minimum 255))
  ([value minimum maximum]
   (-> value
       (min maximum)
       (max minimum))))


(defn ^:private default-hex-range
  "Converts integer to nearest 0 - 255 hex value."
  [value]
  (-> value restrict-range int-to-hex))


(defn ^:private channel-a-b
  [channel]
  (case channel
    :a "A"
    :b "B"
    :both "T"))


(defn back-color
  "|------|-------------------------------------------------------------|
   |color | :bars :white :yellow :cyan :green :magenta :red :blue :black|
   |gain  | 0 - 255                                                     |

   Background color."
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
  "|--------|------------|
   |channel | :a :b :both|
   |red     | 0 - 255    |
   |blue    | 0 - 255    |

   Color correct."
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
  "|--------|------------|
   |channel | :a :b :both|
   |gain    | 0 - 255    |

   Color correct gain."
  [channel gain]
  (format "VCG:%s%s"
          (channel-a-b channel)
          (default-hex-range gain)))


(defn negative
  "|--------|-----------|
   |channel | :a :b     |
   |on-off  | true false|

   Negative effect."
  [channel on-off]
  (format "VDE:%sNG%s"
          (channel-a-b channel)
          (if on-off "NN" "FF")))


(defn mosaic
  "|--------|-------|
   |channel | :a :b |
   |size    | 0 - 30|

   Mosaic effect."
  [channel size]
  (let [value (if size
                (-> size (restrict-range 0 30) int-to-hex)
                "OF")]
    (format "VDE:%sMS%s"
            (channel-a-b channel)
            value)))


(defn mono
  "|--------|-----------|
   |channel | :a :b     |
   |on-off  | true false|

   Mono effect."
  [channel on-off]
  (format "VDE:%sMN%s" (channel-a-b channel) (if on-off "N" "F")))


(defn strobe
  "|---------|-------|
   |channel  | :a :b |
   |slowness | 0 - 62|

   Strobe effect."
  [channel slowness]
  (format "VDE:%sSR%s"
          (channel-a-b channel)
          (if slowness
            (-> slowness (restrict-range 0 62) int-to-hex)
            "OF")))


(defn auto-fade
  "|-------|--------|
   |frames | 0 - 999|

   Execute fade at fixed rate."
  [frames]
  (format "VFA:%s" (-> frames (restrict-range 0 999) (pad-with-zeros 3))))


(defn source-select
  "|--------|------|
   |channel | :a :b|
   |input   | 1 - 5|

   Select video input."
  [channel input]
  (format "VCP:%s%s"
          (channel-a-b channel)
          (restrict-range input 1 5)))


(defn mix-level
  "|------|--------|
   |level | 0 - 255|

   Level of mix lever."
  [level]
  (format "VMM:%s" (default-hex-range level)))
