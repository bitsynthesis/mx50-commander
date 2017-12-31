(ns mx50-commander.command
  "Command wrappers which return strings per the WJ-MX50 RS232 spec."
  (:require [mx50-commander.convert :refer :all]))


(declare wipe-number)


(def ^:private channel-code
  {:a    "A"
   :b    "B"
   :both "T"})


(defn back-color
  "|-------|---------|------------------------|
   | red   | 0 - 255 | value of red channel   |
   | green | 0 - 255 | value of green channel |
   | blue  | 0 - 255 | value of blue channel  |

   Set background matte color using RGB instead of the native YPbPr.
   This command is ignored if back-color-preset is set to :bars."
  {:added "MX30"}
  [red green blue]
  (let [[y p-b p-r] (map default-hex-range (rgb-to-ypbpr red green blue))]
    (format "VBM:%s%s%s" y p-r p-b)))


(defn back-color-preset
  "|-------|--------------------------------------------------------------|
   | color | :bars :white :yellow :cyan :green :magenta :red :blue :black |
   | gain  | 0 - 255                                                      |
   "
  {:added "MX30"}
  [color gain]
  (let [color-code {:bars    "CB"
                    :white   "WH"
                    :yellow  "YL"
                    :cyan    "CY"
                    :green   "GR"
                    :magenta "MG"
                    :red     "RD"
                    :blue    "BU"
                    :black   "BL"}]
    (format "VBC:%s%s"
            (color-code color)
            (default-hex-range gain))))


(defn color-correct
  "|---------|-------------|------------------------|
   | channel | :a :b :both |                        |
   | red     | 0 - 255     | value of red channel   |
   | green   | 0 - 255     | value of green channel |
   | blue    | 0 - 255     | value of blue channel  |

   Set color correct joystick position."
  {:added "MX30"}
  [channel r g b]
  (let [[_ p-b p-r] (rgb-to-ypbpr r g b)]
    (format "VCC:%s%s%s"
            (channel-code channel)
            (default-hex-range p-b)
            (default-hex-range p-r))))


(defn color-correct-gain
  "|---------|-------------|
   | channel | :a :b :both |
   | gain    | 0 - 255     |
   "
  {:added "MX30"}
  [channel gain]
  (format "VCG:%s%s"
          (channel-code channel)
          (default-hex-range gain)))


(defn color-correct-off
  "|---------|-------------|
   | channel | :a :b :both |

   Turn color correct off."
  {:added "MX30"}
  [channel]
  (format "VCC:%sOF" (channel-code channel)))


;; TODO doc
(defn color-correct-y
  {:added "MX70"}
  [channel setup gain]
  (format "VCY:%s%s%s"
          (channel-code channel)
          (default-hex-range setup)
          (default-hex-range gain)))


(defn fade
  "|----------|---------|------------------|
   | duration | 0 - 999 | number of frames |

   Start an automatic fade with the given duration in frames."
  {:added "MX30"}
  [duration]
  (format "VFA:%s" (-> duration (restrict-range 0 999) (zero-pad 3))))


(defn fade-level
  "|-------|---------|---------------------|
   | level | 0 - 255 | 0 = off, 255 = full |

   Level of manual fade slider."
  {:added "MX30"}
  [level]
  (format "VFM:%s" (default-hex-range level)))


(defn fade-settings
  "|--------------|---------------|------------------------------|
   | fade-to      | :white :black | matte source to fade to      |
   | video-on-off | true false    | enable video fading          |
   | dsk-on-off   | true false    | enable downstream key fading |
   | audio-on-off | true false    | enable audio fading          |

   Configure fade settings."
  {:added "MX30"}
  [fade-to video-on-off dsk-on-off audio-on-off]
  ;; TODO matte
  (let [fade-code {:white "W" :black "L"}]
    (format "VFD:%s%s%s%s"
            (fade-code fade-to)
            (if video-on-off "V" "F")
            (if dsk-on-off "T" "F")
            (if audio-on-off "A" "F"))))


(defn fx-decay
  "|---------|--------------|-------------------|
   | channel | :a :b        |                   |
   | frames  | 0 - 20 false | duration of decay |
   "
  {:added "MX70"}
  [channel frames]
  (let [value (if frames
                (-> frames (restrict-range 0 20) int-to-hex)
                "OF")]
    (format "VDE:%sDC%s"
            (channel-code channel)
            value)))


(defn fx-defocus
  "|---------|--------------|
   | channel | :a :b        |
   | level   | 0 - 7 false  |
   "
  {:added "MX70"}
  [channel level]
  (let [value (if level
                (-> level (restrict-range 0 7) (int-to-hex 1))
                "F")]
    (format "VDE:%sDF%s"
            (channel-code channel)
            value)))


(defn fx-mono
  "|---------|------------|--------------------|
   | channel | :a :b      |                    |
   | on-off  | true false | enable mono effect |
   "
  {:added "MX30"}
  [channel on-off]
  (format "VDE:%sMN%s" (channel-code channel) (if on-off "N" "F")))


(defn fx-mirror-h
  "|---------|------------|---------------------------------|
   | channel | :a :b      |                                 |
   | on-off  | true false | enable horizontal mirror effect |
   "
  {:added "MX70"}
  [channel on-off]
  (format "VDE:%sRH%s" (channel-code channel) (if on-off "N" "F")))


(defn fx-mirror-v
  "|---------|------------|-------------------------------|
   | channel | :a :b      |                               |
   | on-off  | true false | enable vertical mirror effect |
   "
  {:added "MX70"}
  [channel on-off]
  (format "VDE:%sRV%s" (channel-code channel) (if on-off "N" "F")))


(defn fx-mosaic
  "|---------|--------------|----------------|
   | channel | :a :b        |                |
   | size    | 0 - 30 false | size of blocks |
   "
  {:added "MX30"}
  [channel size]
  (let [value (if size
                (-> size (restrict-range 0 30) int-to-hex)
                "OF")]
    (format "VDE:%sMS%s"
            (channel-code channel)
            value)))


(defn fx-multi
  "|---------|------------|----------------------------------|
   | channel | :a :b      |                                  |
   | panes   | 1 4 9 16   | size of grid                     |
   | once    | true false | disable repeat                   |
   | speed   | 0 - 62     | frame delay between pane updates |

   Multi screen effect."
  ;; TODO added
  ([channel panes] (fx-multi channel panes true 0))
  ([channel panes once] (fx-multi channel panes once 0))
  ([channel panes once speed]
   (let [pane-code {1  "F"
                    4  "1"
                    9  "2"
                    16 "3"}]
     (format "VDM:%s%s%s%s"
             (channel-code channel)
             (pane-code panes)
             (if once "N" "R")
             (-> speed (restrict-range 0 62) int-to-hex)))))


;; TODO doc
(defn fx-on
  [channel on-off]
  {:added "MX30"}
  (format "VDE:%s%s"
          (channel-code channel)
          (if on-off "ON" "OF")))


(defn fx-negative-c
  "|---------|------------|------------------------|
   | channel | :a :b      |                        |
   | on-off  | true false | enable negative effect |
   "
  ;; TODO added
  [channel on-off]
  (format "VDE:%sNG%s"
          (channel-code channel)
          (if on-off "FF" "FN")))


(defn fx-negative-y
  "|---------|------------|------------------------|
   | channel | :a :b      |                        |
   | on-off  | true false | enable negative effect |
   "
  ;; TODO added
  [channel on-off]
  (format "VDE:%sNG%s"
          (channel-code channel)
          (if on-off "FF" "NF")))


(defn fx-negative
  "|---------|------------|------------------------|
   | channel | :a :b      |                        |
   | on-off  | true false | enable negative effect |
   "
  {:added "MX30"}
  [channel on-off]
  (format "VDE:%sNG%s"
          (channel-code channel)
          (if on-off "NN" "FF")))


(defn fx-strobe
  "|----------|--------------|--------------------------------|
   | channel  | :a :b        |                                |
   | slowness | 0 - 62 false | delay between frames in frames |
   "
  {:added "MX30"}
  [channel slowness]
  (format "VDE:%sSR%s"
          (channel-code channel)
          (if slowness
            (-> slowness (restrict-range 0 62) int-to-hex)
            "OF")))


(defn input
  "|---------|-------|--------------------------|
   | channel | :a :b |                          |
   | input   | 0 - 4 | 0 = matte, 1 - 4 = input |

   Select video input."
  {:added "MX30"}
  [channel input]
  (format "VCP:%s%s"
          (channel-code channel)
          (if (zero? input)
            "C"
            (restrict-range input 1 4))))


;; TODO doc
(defn key-fx
  {:added "MX70"}
  ([effect] (key-fx effect 0))
  ([effect frames]
   (let [effect-code {:border        "TF2"
                      :border-spark  "TF3"
                      :none          "FF0"
                      :self          "TF0"
                      :self-spark    "TF1"
                      :shadow        "SF0"}]
     (format "VKX:%s%s"
             (effect-code effect)
             (-> frames (restrict-range 1 32) zero-pad)))))


;; TODO doc
(defn key-level
  ;; TODO added
  [level]
  (format "VKL:%s" (default-hex-range level)))


;; TODO doc
(defn key-on
  {:added "MX70"}
  [variation]
  (let [variation-code {:chroma 62
                        :luma   61
                        :ext    59}]
    (wipe-number (variation-code variation))))


;; TODO doc
(defn key-slice-slope
  {:added "MX70"}
  [slice slope]
  (format "VKS:%s%s"
          (default-hex-range slice)
          (-> slope (restrict-range 0 15) (int-to-hex 1))))


(defn position
  "|---|---------|
   | x | 0 - 255 |
   | y | 0 - 255 |

   Set pip joystick position."
  {:added "MX30"}
  [x y]
  (format "VPS:N%s%s"
          (default-hex-range x)
          (default-hex-range y)))


(defn power
  "|-----------|------------|----------------|
   | always-on | true false | false = toggle |
   "
  {:added "MX30"}
  ([] (power false))
  ([always-on]
   (if always-on "PON" "POF")))


(defn wipe
  "|----------|---------|--------|
   | duration | 0 - 999 | frames |

   Start an automatic wipe with the given duration in frames."
  {:added "MX30"}
  [duration]
  (format "VMA:%s" (-> duration (restrict-range 0 999) (zero-pad 3))))


(defn wipe-border
  "|--------|-------------------------------------|----------------|
   | border | :none :border :border2 :soft :soft2 | type of border |
   "
  {:added "MX30"}
  [border]
  (let [border-code {:none    "OF"
                     :border  "B1"
                     :border2 "B2"
                     :soft    "S1"
                     :soft2   "S2"}]
    (format "VWB:%s" (border-code border))))


(defn wipe-level
  "|-------|---------|---------------------|
   | level | 0 - 255 | 0 = off, 255 = full |

   Position of wipe lever."
  {:added "MX30"}
  [level]
  (format "VMM:%s" (default-hex-range level)))


(defn wipe-one-way
  "|--------|------------|------------------------------|
   | on-off | true false | enable one-way wipe modifier |
   "
  {:added "MX30"}
  [on-off]
  (format "VWD:X%s" (if on-off "N" "F")))


(defn wipe-pattern
  "|----------|------------|--------------------------------------------------|
   | button   | 0 - 6      | wipe button from left to right on panel          |
   | pattern  | 0 - 3      | number of additional presses on the given button |
   | modifier | see source | keyword corresponding to wipe modifier buttons   |
   "
  {:added "MX30"}
  ([button pattern] (wipe-pattern button pattern :none))
  ([button pattern modifier]
   (let [button-root [1 5 16 20 9 24 12]
         modifier-code {:none           "MLF"
                        :compression    "ZM1"
                        :compression2   "ZM2"
                        :slide          "SC1"
                        :slide2         "SC2"
                        ;; TODO
                        :pairing
                        :blinds
                        :multi          "ML1"
                        :multi2         "ML2"
                        :multi3         "ML3"
                        :multi4         "ML4"
                        :multi5         "ML5"
                        :multi6         "ML6"
                        :multi-pairing  "MP1"
                        :multi-pairing2 "MP2"
                        :multi-pairing3 "MP3"
                        :multi-pairing4 "MP4"
                        :multi-pairing5 "MP5"
                        :multi-pairing6 "MP6"}]
     (format "VWP:%s%s"
             (-> button button-root (+ pattern) (restrict-range 0 27) zero-pad)
             (modifier-code modifier)))))


;; TODO docs
(defn wipe-number
  {:added "MX70"}
  [number]
  (format "VWN:%s" (zero-pad number 4)))


(defn wipe-reverse
  "|--------|------------|------------------------------|
   | on-off | true false | enable reverse wipe modifier |
   "
  {:added "MX30"}
  [on-off]
  (format "VWD:%sX" (if on-off "N" "F")))
