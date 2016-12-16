(ns mx50-commander.command
  (:require [mx50-commander.convert :refer :all]))


(defn back-color
  "|-------|---------|------------------------|
   | red   | 0 - 255 | value of red channel   |
   | green | 0 - 255 | value of green channel |
   | blue  | 0 - 255 | value of blue channel  |

   Set background matte color using RGB instead of the native YPbPr.
   "
  [red green blue]
  (let [[y p-r p-b] (map default-hex-range (rgb-to-ypbpr red green blue))]
    (format "VBM:%s%s%s" y p-r p-b)))


(defn back-color-preset
  "|-------|--------------------------------------------------------------|
   | color | :bars :white :yellow :cyan :green :magenta :red :blue :black |
   | gain  | 0 - 255                                                      |
   "
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


;; TODO rgb instead... maybe in conjunction with cc gain
(defn color-correct
  "|---------|-------------|
   | channel | :a :b :both |
   | p-r     | 0 - 255     |
   | p-b     | 0 - 255     |
   "
  [channel p-r p-b]
  (format "VCC:%s%s%s"
          (channel-a-b channel)
          (default-hex-range p-r)
          (default-hex-range p-b)))


;; TODO this may just be <y> of CC <p-r> <p-b>
(defn color-correct-gain
  "|---------|-------------|
   | channel | :a :b :both |
   | gain    | 0 - 255     |
   "
  [channel gain]
  (format "VCG:%s%s"
          (channel-a-b channel)
          (default-hex-range gain)))


(defn color-correct-off
  "|---------|-------------|
   | channel | :a :b :both |

   Turn color correct off."
  [channel]
  (format "VCC:%sOF" (channel-a-b channel)))


(defn fx-mono
  "|---------|------------|--------------------|
   | channel | :a :b      |                    |
   | on-off  | true false | enable mono effect |
   "
  [channel on-off]
  (format "VDE:%sMN%s" (channel-a-b channel) (if on-off "N" "F")))


(defn fx-mosaic
  "|---------|--------|----------------|
   | channel | :a :b  |                |
   | size    | 0 - 30 | size of blocks |
   "
  [channel size]
  (let [value (if size
                (-> size (restrict-range 0 30) int-to-hex)
                "OF")]
    (format "VDE:%sMS%s"
            (channel-a-b channel)
            value)))


(defn fx-multi
  "|---------|------------|----------------------------------|
   | channel | :a :b      |                                  |
   | panes   | 1 4 9 16   | size of grid                     |
   | once    | true false | disable repeat                   |
   | speed   | 0 - 62     | frame delay between pane updates |

   Multi screen effect."
  ([channel panes] (fx-multi channel panes true 0))
  ([channel panes once] (fx-multi channel panes once 0))
  ([channel panes once speed]
   (let [once-code {:once "N" :repeat "R"}
         pane-code {1 "F"
                    4 "1"
                    9 "2"
                    16 "3"}]
     (format "VDM:%s%s%s%s"
             (channel-a-b channel)
             (pane-code panes)
             (once-code once)
             (-> speed (restrict-range 0 62) int-to-hex)))))


(defn fx-negative
  "|---------|------------|------------------------|
   | channel | :a :b      |                        |
   | on-off  | true false | enable negative effect |
   "
  [channel on-off]
  (format "VDE:%sNG%s"
          (channel-a-b channel)
          (if on-off "NN" "FF")))


(defn fx-strobe
  "|----------|--------|--------------------------------|
   | channel  | :a :b  |                                |
   | slowness | 0 - 62 | delay between frames in frames |
   "
  [channel slowness]
  (format "VDE:%sSR%s"
          (channel-a-b channel)
          (if slowness
            (-> slowness (restrict-range 0 62) int-to-hex)
            "OF")))


(defn input
  "|---------|-------|--------------------------|
   | channel | :a :b |                          |
   | input   | 0 - 4 | 0 = matte, 1 - 4 = input |

   Select video input."
  [channel input]
  (format "VCP:%s%s"
          (channel-a-b channel)
          (if (zero? input)
            "C"
            (restrict-range input 1 4))))


(defn wipe
  "|----------|---------|--------|
   | duration | 0 - 999 | frames |

   Start an automatic wipe with the given duration in frames."
  [duration]
  (format "VMA:%s" (-> duration (restrict-range 0 999) (pad-with-zeros 3))))


(defn wipe-border
  "|--------|-------------------------------------|----------------|
   | border | :none :border :border2 :soft :soft2 | type of border |
   "
  [border]
  (let [border-code {:none "OF"
                     :border "B1"
                     :border2 "B2"
                     :soft "S1"
                     :soft2 "S2"}]
    (format "VWB:%s" (border-code border))))


(defn wipe-level
  "|-------|---------|---------------------|
   | level | 0 - 255 | 0 = off, 255 = full |

   Position of wipe lever."
  [level]
  (format "VMM:%s" (default-hex-range level)))


(defn wipe-one-way
  "|--------|------------|------------------------------|
   | on-off | true false | enable one-way wipe modifier |
   "
  [on-off]
  (format "VWD:X%s" (if on-off "N" "F")))


(defn wipe-pattern
  "|----------|------------|--------------------------------------------------|
   | button   | 0 - 6      | wipe button from left to right on panel          |
   | pattern  | 0 - 3      | number of additional presses on the given button |
   | modifier | see source | keyword corresponding to wipe modifier buttons   |
   "
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


(defn wipe-reverse
  "|--------|------------|------------------------------|
   | on-off | true false | enable reverse wipe modifier |
   "
  [on-off]
  (format "VWD:%sX" (if on-off "N" "F")))


(defn fade
  "|----------|---------|------------------|
   | duration | 0 - 999 | number of frames |

   Start an automatic fade with the given duration in frames."
  [duration]
  (format "VFA:%s" (-> duration (restrict-range 0 999) (pad-with-zeros 3))))


(defn fade-level
  "|-------|---------|---------------------|
   | level | 0 - 255 | 0 = off, 255 = full |

   Level of manual fade slider."
  [level]
  (format "VFM:%s" (default-hex-range level)))


(defn fade-settings
  "|--------------|---------------|------------------------------|
   | fade-to      | :white :black | matte source to fade to      |
   | video-on-off | true false    | enable video fading          |
   | dsk-on-off   | true false    | enable downstream key fading |
   | audio-on-off | true false    | enable audio fading          |

   Configure fade settings."
  [fade-to video-on-off dsk-on-off audio-on-off]
  ;; TODO matte
  (let [fade-code {:white "W" :black "L"}]
    (format "VFD:%s%s%s%s"
            (fade-code fade-to)
            (if video-on-off "V" "F")
            (if dsk-on-off "T" "F")
            (if audio-on-off "A" "F"))))
