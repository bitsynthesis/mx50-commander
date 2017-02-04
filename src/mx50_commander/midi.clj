(ns mx50-commander.midi
  "MIDI async to sync, dropping older messages for newer while consumer is
   blocked."
  (:require [clojure.core.async :refer [>!! >! <!! <!] :as a]
            [midi :as m]
            [mx50-commander.control :as con]
            [mx50-commander.command :as cmd]
            [mx50-commander.generate :as gen]))


(declare midi-stop)


(def ^:private midi-listeners (atom {}))
(def ^:private midi-in (atom nil))


(defn ^:private create-midi-buffer []
  (let [number-of-events-buffered 8]
    (a/chan (a/sliding-buffer number-of-events-buffered))))


(defn ^:private apply-listener [l event]
  (let [event-matcher (if (set? (:filter l))
                        (:type event)
                        event)]
    (when (and (or (nil? (:chan l))
                   (= (:chan l) (:chan event)))
               ((:filter l) event-matcher))
      (>!! (:buffer l) event))))


(defn ^:private assoc-event-metadata [event timestamp]
  (assoc event
         :time timestamp
         ;; http://www.midimountain.com/midi/midi_status.htm
         :type (case (:cmd event)
                 128 :note-off
                 144 :note-on
                 160 :poly-aftertouch
                 176 :control-change
                 192 :program-change
                 224 :pitch-wheel
                 240 :sysex
                 :unknown)))


(defn ^:private midi-handler
  [event timestamp]
  (let [enriched-event (assoc-event-metadata event timestamp)]
    (doseq [[id l] @midi-listeners]
      (apply-listener l enriched-event))))


(defn ^:private create-midi-consumer [l]
  (a/go
   (loop []
     (when-let [midi-event (<! (:buffer l))]
       ((:handler l) (:device l) midi-event)
       (recur)))))


;; TODO doc
(defn midi-stop []
  (doseq [[id l] @midi-listeners]
    (a/close! (:buffer l))))


;; TODO doc
(defn midi-start
  ([] (midi-start false))
  ([reselect]
   (midi-stop)
   (doseq [[id l] @midi-listeners]
     (swap! midi-listeners assoc-in [id :buffer] (create-midi-buffer)))

   (when (or (nil? @midi-in) reselect)
     ;; when a device has previously been selected, stub it out
     (when @midi-in
       (m/midi-handle-events @midi-in (fn [& _])))
     (if-let [in (m/midi-in)]
       (do
         (reset! midi-in in)
         (m/midi-handle-events in midi-handler))
       (println "No MIDI device found.")))
   (doseq [[id l] @midi-listeners]
     (create-midi-consumer l))))


(def ^:private default-listener-params
  {:chan nil
   :device nil
   :filter #{:note-on :note-off}})


;; TODO doc
(defn listener
  [id params]
  (as-> params |
   (merge default-listener-params |)
   (assoc | :buffer (create-midi-buffer))
   (swap! midi-listeners assoc id |)))


(def ^:private default-kit-params
  {:first-note 36
   :bank-size 16})


(defn ^:private bank-handler
  [params]
  (fn [device event]
   (when (<= (:first-note params) (:note event))
     (let [relative-note (- (:note event) (:first-note params))
           relative-bank (int (/ relative-note 16))
           bank-note (rem relative-note 16)]
       (when-let [b (nth (:banks params) relative-bank)]
         (b device (assoc event :bank-note bank-note)))))))


;; TODO doc
(defn kit
  [id params]
  (listener id (assoc params :handler (bank-handler params))))
