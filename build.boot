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
         '[codox.boot :as codox])


(def version "0.3")


(def public-namespaces
  '#{mx50-commander.command
     mx50-commander.control
     mx50-commander.convert
     mx50-commander.filter
     mx50-commander.generate
     mx50-commander.midi})


(task-options!
 aot {:namespace public-namespaces}
 pom {:project 'bbakersmith/mx50-commander
      :version version})


(deftask test []
  (boot-test/test))


(deftask doc []
  (comp (codox/codox
         :name "MX50 Commander"
         :description (str "A Clojure library for controlling "
                           "Panasonic WJ-MX50 and WJ-MX30 "
                           "video mixers via USB RS232 serial "
                           "port adapter.")
         :metadata {:doc/format :markdown}
         :source-uri (str "https://github.com"
                          "/bbakersmith/mx50-commander"
                          "/blob/{version}/{filepath}#L{line}")
         :version version)
        (target)))


(deftask aot-repl []
  (comp
   (aot :namespace '#{mx50-commander.control})
   (uber)
   (jar)
   (target)
   (repl)))


(deftask build []
  (comp (aot) (pom) (jar) (install)))
