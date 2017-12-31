(set-env!
 :source-paths #{"src"}
 :resource-paths #{"test"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [adzerk/boot-test "1.1.1" :scope "test"]
                 [boot-codox "0.10.2" :scope "test"]
                 [org.clojure/core.async "0.2.374"]
                 [org.scream3r/jssc "2.8.0"]])


(require '[adzerk.boot-test :as boot-test]
         '[codox.boot :as codox])


(def version "0.6")


(def public-namespaces
  '#{mx50-commander.classes
     mx50-commander.command
     mx50-commander.control
     mx50-commander.convert
     mx50-commander.filter
     mx50-commander.generate})


(task-options!
 aot {:namespace public-namespaces}
 pom {:project 'bbakersmith/mx50-commander
      :version version})


(deftask aot-classes []
  (aot :namespace '#{mx50-commander.control}))


(deftask doc []
  (comp
   (aot-classes)
   (codox/codox
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
  (comp (aot-classes) (repl)))


(deftask test []
  (comp (aot-classes) (boot-test/test)))


(deftask build []
  (comp (aot) (pom) (jar) (install)))
