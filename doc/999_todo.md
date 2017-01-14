# TODO

* logging!!!
* is "device" a good name? is it too generic with midi devices in the mix?
* filter out midi cc messages (or convert them)
* rename core ns to device
* documentation
* more better curve generators
* race condition in generator (or generator test at least)



;; HELPER IDEAS BELOW
;;
;; this might work... generate first version of cmd, compare it to current, if same
;; use second version of cmd. tuples required, but could bind multiple vars.
;;
;; (con/toggle [x [true false]]
;;   (mx50 (cmd/fx-negative :b x)))
;;
;; - midi velocity to collection
;;





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


;; TODO ponder...
;; is it ok to require bindings for midi-start? how does this hurt stop / start-
;; ability? would we be able to unify device / midi stop / start functions if this
;; wasn't the case?
;;
;; working just like devices, store bindings so midi-start can recreate consumers...
;;
;; (def midi-channels (atom {}))
;; => {5 psych-kit, 9 gnarly-synth}
;;
;; ex...
;;
;; (midi-binding
;;   5 psych-kit
;;   9 gnarly-synth)
;;
;; (midi-binding 0 lead-synth)
;;
;; (device-stop :mx50)
;; (midi-stop 9)
;; (device-stop)
;; (midi-stop)
;; (stop)

;; TODO support multiple midi devices? helps standardize with mx devices
;; (midi-device :mpc {5 psych-kit 9 gnarly-synth})
;; (midi-stop :mpc 5) ;; maybe...
;; (midi-stop :mpc)
;; (midi-stop)
