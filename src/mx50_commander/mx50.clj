(ns mx50-commander.mx50)


(defn ^:private pad-with-zeros
  ([value] (pad-with-zeros value 2))
  ([value digits]
   (loop [s (str value)]
         (if (< (count s) digits)
           (recur (str "0" s))
           s))))


(defn ^:private int-to-hex
  ([value] (int-to-hex value 2))
  ([value digits]
   (-> value
       java.lang.Integer/toHexString
       .toUpperCase
       (pad-with-zeros digits))))


(defn ^:private restrict-range
  "Limit the range of an integer value."
  [value minimum maximum]
  (-> value
      (min maximum)
      (max minimum)))


(defn ^:private default-hex-range
  "Converts integer to nearest 0 - 255 hex value."
  [value]
  (-> value (restrict-range 0 255) int-to-hex))


(defn ^:private channel-a-b
  [channel]
  (case channel
    :a "A"
    :b "B"
    :both "T"))


(defn back-color-preset
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


(defn rgb-to-yuv
  "Returns [y p-b p-r]"
  [r g b]
  (mapv int
        [(+ (* r 0.257) (* g 0.504) (* b 0.098) 16)
         (+ (* r 0.439) (* g -0.368) (* b -0.071) 128)
         (+ (* r -0.148) (* g -0.291) (* b 0.439) 128)]))


(defn back-color
  ;; TODO docs
  [red green blue]
  (let [[y p-r p-b] (map default-hex-range (rgb-to-yuv red green blue))]
    (format "VBM:%s%s%s" y p-r p-b)))


(defn color-correct
  "|--------|------------|
   |channel | :a :b :both|
   |p-r     | 0 - 255    |
   |p-b     | 0 - 255    |

   Color correct."
  [channel p-r p-b]
  (format "VCC:%s%s%s"
          (channel-a-b channel)
          (default-hex-range p-r)
          (default-hex-range p-b)))


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


(defn input
  "|--------|--------------|
   |channel | :a :b        |
   |input   | 0 (matte) - 4|

   Select video input."
  [channel input]
  (format "VCP:%s%s"
          (channel-a-b channel)
          (if (zero? input)
            "C"
            (restrict-range input 1 4))))


(defn wipe
  "|------|--------|
   |level | 0 - 255|

   Position of wipe lever."
  [level]
  (format "VMM:%s" (default-hex-range level)))


(defn wipe-pattern
  ;; TODO docs
  [button pattern modifier]
  (let [button-root [1 5 16 20 9 24 12]
        modifier-code {:none "MLF"
                       :compression "ZM1"
                       :compression2 "ZM2"
                       :slide "SC1"
                       :slide2 "SC2"
                       :pairing
                       :blinds
                       :multi "ML1"
                       :multi2 "ML2"
                       :multi3 "ML3"
                       :multi4 "ML4"
                       :multi5 "ML5"
                       :multi6 "ML6"
                       :multi-pairing "MP1"
                       :multi-pairing2 "MP2"
                       :multi-pairing3 "MP3"
                       :multi-pairing4 "MP4"
                       :multi-pairing5 "MP5"
                       :multi-pairing6 "MP6"}]
    (format "VWP:%s%s"
            (-> button button-root (+ pattern) (restrict-range 0 27) pad-with-zeros)
            (modifier-code modifier))))


(defn wipe-border
  ;; TODO docs
  [border]
  (let [border-code {:none "OF"
                     :border "B1"
                     :border2 "B2"
                     :soft "S1"
                     :soft2 "S2"}]
    (format "VWB:%s" (border-code border))))


(defn wipe-reverse
  ;; TODO docs
  [on-off]
  (format "VWD:%sX" (if on-off "N" "F")))


(defn wipe-one-way
  ;; TODO docs
  [on-off]
  (format "VWD:X%s" (if on-off "N" "F")))


(defn fade
  ;; TODO docs
  [fade-to video-on-off dsk-on-off audio-on-off]
  ;; TODO matte
  (let [fade-code {:white "W" :black "L"}]
    (format "VFD:%s%s%s%s"
            (fade-code fade-to)
            (if video-on-off "V" "F")
            (if dsk-on-off "T" "F")
            (if audio-on-off "A" "F"))))


(defn fade-manual
  ;; TODO docs
  [level]
  (format "VFM:%s" (-> level (restrict-range 0 255) int-to-hex)))


(defn fade-auto
  ;; TODO docs
  [duration]
  (format "VFA:%s" (-> duration (restrict-range 0 999) (pad-with-zeros 3))))


(defn multi
  ;; TODO docs
  ([channel panes] (multi channel panes :repeat 0))
  ([channel panes mode] (multi channel panes mode 0))
  ([channel panes mode speed]
  (let [mode-code {:once "N" :repeat "R"}
        pane-code {1 "F"
                   4 "1"
                   9 "2"
                   16 "3"}]
    (format "VDM:%s%s%s%s"
            (channel-a-b channel)
            (pane-code panes)
            (mode-code mode)
            (-> speed (restrict-range 0 62) int-to-hex)))))
