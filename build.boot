(set-env!
 :source-paths #{"src"}
 :resource-paths #{"test"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [adzerk/boot-test "1.1.1" :scope "test"]
                 [boot-codox "0.10.2" :scope "test"]
                 [org.clojure/core.async "0.2.374"]
                 [org.scream3r/jssc "2.8.0"]])


(require '[adzerk.boot-test :as boot-test]
         '[codox.boot :refer [codox]])


(def version "0.2")


(deftask test []
  (boot-test/test
   :exclusions #{'mx50-commander.examples.basic}))


(deftask doc []
  (comp (codox :name "MX50 Commander"
               :version version
               :description (str "A Clojure library for controlling "
                                 "Panasonic WJ-MX50 and WJ-MX30 video mixers "
                                 "via USB RS232 serial port adapter.")
               :metadata {:doc/format :markdown}
               :source-uri (str "https://github.com/bbakersmith/mx50-commander"
                                "/blob/{version}/{filepath}#L{line}"))
        (target)))
