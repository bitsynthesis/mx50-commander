(set-env!
 :source-paths #{"src"}
 :resource-paths #{"test"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [adzerk/boot-test "1.1.1" :scope "test"]
                 [boot-codox "0.10.2" :scope "test"]
                 [org.clojure/core.async "0.2.374"]
                 [org.scream3r/jssc "2.8.0"]
                 [overtone/midi-clj "0.1"]])


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


(deftask build []
  ;; TODO not sure aot-ing everything is the right move here
  (comp (aot :namespace #{'mx50-commander.command
                          'mx50-commander.control
                          'mx50-commander.convert
                          'mx50-commander.generate
                          'mx50-commander.midi})
        (pom :project 'bbakersmith/mx50-commander
             :version version)
        (jar)
        (install)))
