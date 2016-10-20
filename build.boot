(set-env!
 :source-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.scream3r/jssc "2.8.0"]])


(deftask build []
  (comp (aot :namespace #{'mx-serial.core})
        (pom :project 'bbakersmith/mx-serial
             :version "1.0.0")
        (jar :main 'mx-serial.core)
        (install)))
