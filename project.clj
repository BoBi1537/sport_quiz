(defproject sport-quiz "0.1.0-SNAPSHOT"
  :description "Sport Quiz - Clojure CLI igra sa više sportskih igara"
  :url "http://localhost:3000"

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [midje "1.10.9"]]

  :plugins [[lein-midje "3.2.2"]]

  :main sport-quiz.core

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})