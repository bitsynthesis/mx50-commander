(ns mx50-commander.convert-test
  (:require [clojure.test :refer :all]
            [mx50-commander.convert :as cvt]))


(deftest hsl-to-rgb
  (let [counter (atom 0)
        test-cases (partition 2 [[0   0   0]   [0   0   0]   ;; black
                                 [0   0   100] [255 255 255] ;; white
                                 [0   100 50]  [255 0   0]   ;; red
                                 [120 100 50]  [0   255 0]   ;; green
                                 [240 100 50]  [0   0   255] ;; blue
                                 [60  100 50]  [255 255 0]   ;; yellow
                                 [180 100 50]  [0   255 255] ;; cyan
                                 [300 100 50]  [255 0   255] ;; magenta
                                 [0   0   75]  [192 192 192] ;; silver
                                 [0   0   50]  [128 128 128] ;; grey
                                 [0   100 25]  [128 0   0]   ;; dark red
                                 [120 100 25]  [0   128 0]   ;; dark green
                                 [240 100 25]  [0   0   128] ;; dark blue
                                 [60  100 25]  [128 128 0]   ;; dark yellow
                                 [180 100 25]  [0   128 128] ;; dark cyan
                                 [300 100 25]  [128 0   128] ;; dark magenta
                                 ])]
    (doseq [[hsl rgb] test-cases]
      (swap! counter inc)
      (is (= rgb (apply cvt/hsl-to-rgb hsl))))
    ;; guards against doseq not running at all
    (is (< 0 @counter))
    (is (= (count test-cases) @counter))))


(deftest relative
  (is (= [:a :a :b :c :c]
         (map (partial cvt/relative [:a :b :c] 8) [0 2 3 6 9]))))
