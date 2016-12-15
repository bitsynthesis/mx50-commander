(ns mx50-commander.generators-test
  (:require [clojure.test :refer :all]
            [mx50-commander.core :as core]
            [mx50-commander.generators :as gen]
            [mx50-commander.mx50 :as mx]
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
         (gen/filter-bindings keyword? '('x 123 :a 1 'y 456 :b str)))))


(deftest do-generate
  (let [test-id :test-device-1
        test-device (core/device test-id)]
    (gen/do-generate [r (range 255)
                      b (range 127)
                      :type gen/linear
                      :steps 4]
      (test-device (mx/color-correct :b r b)))
    (is (= ["VCC:B00"
            "VCC:B00"
            "VCC:B00"
            "VCC:B00"]
           @shared/cmds-sent))))
