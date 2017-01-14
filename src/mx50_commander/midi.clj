(ns mx50-commander.midi
  "MIDI async to sync, dropping older messages for newer while consumer is
   blocked."
  (:require [clojure.core.async :refer [>!! <!] :as a]
            [midi :as m]
            [mx50-commander.control :as con]
            [mx50-commander.command :as cmd]
            [mx50-commander.generate :as gen]))


(declare midi-stop)


(def ^:private midi-buffers (atom nil))
(def ^:private midi-in (atom nil))


(defn ^:private create-midi-buffers []
  (reset! midi-buffers (repeatedly 16 #(a/chan (a/sliding-buffer 1)))))


(defn ^:private midi-handler
  [message timestamp]
  (>!! (nth @midi-buffers (:chan message))
       (assoc message :time timestamp)))


(defn ^:private create-midi-consumer [chan handler]
  (let [async-chan (nth @midi-buffers chan)]
    (a/go
     (loop []
       (when-let [midi-event (<! async-chan)]
         (handler midi-event)
         (recur))))))


;; TODO doc
(defn midi-stop []
  (doall (map a/close! @midi-buffers)))


;; TODO naming, move to control?
(defn midi-start
  ([bindings] (midi-start bindings false))
  ([bindings reselect]
   (midi-stop)
   (create-midi-buffers)

   ;; TODO revisit this midi-in approach to only calling
   ;; m/midi-in once successfully, and not requiring
   ;; users to do so...
   ;;
   ;; instead maybe move the when-not to midi-triggers
   ;; and build in closing logic here, if closing is a thing
   ;; with m/midi-in... then can expose midi-start publicly
   ;; in case users need to trigger a manual refresh of the
   ;; midi input - say, to select a different device
   (when (or (nil? @midi-in) reselect)
     ;; when a device has previously been selected, stub it out
     (when @midi-in
       (m/midi-handle-events @midi-in identity))
     (if-let [in (m/midi-in)]
       (do
         (reset! midi-in in)
         (m/midi-handle-events in midi-handler))
       (println "No MIDI device found.")))

   (doseq [[chan handler] bindings]
     (create-midi-consumer chan handler))))
