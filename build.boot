(set-env!
 :source-paths #{"src"}
 :resource-paths #{"test"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [adzerk/boot-test "1.1.1" :scope "test"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.scream3r/jssc "2.8.0"]])


(require '[adzerk.boot-test :as boot-test])


(deftask test []
  (boot-test/test))


(deftask build []
  (comp (aot :namespace #{'mx50-commander.core})
        (pom :project 'bbakersmith/mx50-commander
             :version "2.0.0")
        (jar :main 'mx50-commander.core)
        (install)))
