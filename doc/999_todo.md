# TODO

* logging!!!
* is "device" a good name? is it too generic with midi devices in the mix?
* filter out midi cc messages (or convert them)
* rename core ns to device
* documentation
* more better curve generators
* race condition in generator (or generator test at least)



;; TODO move to todo
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
