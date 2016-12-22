(ns mx50-commander.midi
  (:require [clojure.core.async :refer [>!! <!!] :as a]
            [midi :as m]
            [mx50-commander.control :as con]
            [mx50-commander.command :as cmd]
            [mx50-commander.generate :as gen]))


(def ^:private midi-buffers (atom nil))
(def ^:private midi-in (atom nil))

(defn ^:private reset-midi-buffers []
  (doall (map a/close! @midi-buffers))
  (reset! midi-buffers (repeatedly 16 #(a/chan (a/sliding-buffer 1)))))

(defn ^:private midi-handler
  [message timestamp]
  (>!! (nth @midi-buffers (:chan message))
       (assoc message :time timestamp)))

(defn ^:private create-midi-consumer [chan handler]
  (let [async-chan (nth @midi-buffers chan)]
    (a/go
     (loop []
       (when-let [midi-event (<!! async-chan)]
         (handler midi-event)
         (recur))))))

(defn ^:private midi-start []
  ;; TODO revisit this midi-in approach to only calling
  ;; m/midi-in once successfully, and not requiring
  ;; users to do so...
  ;;
  ;; instead maybe move the when-not to midi-triggers
  ;; and build in closing logic here, if closing is a thing
  ;; with m/midi-in... then can expose midi-start publicly
  ;; in case users need to trigger a manual refresh of the
  ;; midi input - say, to select a different device
  (when-not @midi-in
    (if-let [in (m/midi-in)]
      (do
        (reset! midi-in in)
        (m/midi-handle-events in midi-handler))
      (println "No MIDI device found."))))

;; TODO naming, move to control
(defn midi-triggers [& bindings]
  (reset-midi-buffers)
  (midi-start)
  (doseq [[chan handler] (partition 2 bindings)]
    (create-midi-consumer chan handler)))


;; TODO move to examples
;; DEMO START

;;
;; (def mx50
;;   (con/device :mx50 {:port "/dev/ttyUSB1" :cache true :rate 50}))
;;
;; (defn gross-synth [event]
;;   (mx50 (cmd/back-color (-> event :vel (- 64) (* 4)) 255 0)))
;;
;; (defn psych-kit [event]
;;   (case (:note event)
;;     44 (mx50 (cmd/wipe-level (* 1.25 (:vel event))))
;;     45 (mx50 (cmd/fx-negative :b (< (:vel event) 65)))
;;     46 (gen/generate [_ (range 4)
;;                       r (range 100 225)
;;                       g (range 150)]
;;         (mx50 (cmd/back-color r g 0)))
;;     :do-nothing))
;;
;; (midi-triggers
;;  0 gross-synth
;;  1 psych-kit)
;;

;; DEMO END
