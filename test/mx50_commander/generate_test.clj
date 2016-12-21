(ns mx50-commander.generate-test
  (:require [clojure.test :refer :all]
            [mx50-commander.control :as con]
            [mx50-commander.generate :as gen]
            [mx50-commander.command :as cmd]
            [mx50-commander.test-shared :as shared]))


(use-fixtures :each shared/each-fixture)


(deftest linear-generator
  (is (= [[0 :red]
          [10 :red]
          [19 :blue]
          [29 :blue]]
         (gen/linear 4 (range 30) [:red :blue]))))


(deftest curve-generator
  (is (= [[0 :red]
          [5 :blue]
          [14 :yellow]
          [29 :yellow]]
         (gen/curve 4 (range 30) [:red :blue :yellow]))))


(deftest filter-bindings
  (is (= {:a 1 :b 'str}
         (#'gen/filter-bindings keyword? '('x 123 :a 1 'y 456 :b str)))))


(deftest generate
  (let [test-id :test-device-1
        test-device (con/device test-id)]
    (con/start test-id)
    (gen/generate [_ (range 4)
                   panes [1 4 9 16]
                   speed (range 63)]
      (test-device (cmd/fx-multi :b panes false speed)))
    (Thread/sleep 100)
    (is (= ["VDM:BFR00"
            "VDM:B1R15"
            "VDM:B2R29"
            "VDM:B3R3E"]
           @shared/cmds-sent))))
