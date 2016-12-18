(ns mx50-commander.generate-test
  (:require [clojure.test :refer :all]
            [mx50-commander.core :as core]
            [mx50-commander.generate :as gen]
            [mx50-commander.command :as cmd]
            [mx50-commander.test-shared :as shared]))


(use-fixtures :each shared/each-fixture)


(deftest linear-generator
  (is (= [[:b 0 :red]
          [:b 10 :red]
          [:b 19 :blue]
          [:b 29 :blue]]
         (gen/linear 4 :b (range 30) [:red :blue]))))


(deftest curve-generator
  (is (= [[:b 0 :red]
          [:b 5 :blue]
          [:b 14 :yellow]
          [:b 29 :yellow]]
         (gen/curve 4 :b (range 30) [:red :blue :yellow]))))


(deftest filter-bindings
  (is (= {:a 1 :b 'str}
         (#'gen/filter-bindings keyword? '('x 123 :a 1 'y 456 :b str)))))


(deftest generate
  (let [test-id :test-device-1
        test-device (core/device test-id)]
    (core/start test-id)
    (gen/generate [panes [1 4 9 16]
                   once false
                   speed (range 63)
                   :type gen/linear
                   :steps 4]
      (test-device (cmd/fx-multi :b panes once speed)))
    (Thread/sleep 100)
    (is (= ["VDM:BFR00"
            "VDM:B1R15"
            "VDM:B2R29"
            "VDM:B3R3E"]
           @shared/cmds-sent))))
