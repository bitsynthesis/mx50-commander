(set-env!
 :source-paths #{"src"}
 :resource-paths #{"test"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [adzerk/boot-test "1.1.1" :scope "test"]
                 [boot-codox "0.10.2" :scope "test"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.scream3r/jssc "2.8.0"]])


(require '[adzerk.boot-test :as boot-test]
         '[codox.boot :refer [codox]])


(deftask test []
  (boot-test/test))


(deftask doc []
  (comp (codox :name "MX50 Commander"
               :version "2.1"
               :description "
A Clojure library for controlling Panasonic WJ-MX50 and WJ-MX30 video mixers
via USB serial port adapter."
               :metadata {:doc/format :markdown}
               :source-uri "https://github.com/bbakersmith/mx50-commander/blob/{version}/{filepath}#L{line}"
               )
        (target)))


(deftask build []
  (comp (aot :namespace #{'mx50-commander.core})
        (pom :project 'bbakersmith/mx50-commander
             :version "2.1")
        (jar :main 'mx50-commander.core)
        (install)))
