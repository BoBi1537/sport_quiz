(defproject sport-quiz "0.1.0-SNAPSHOT"
  :description "Sport Quiz - Clojure web + cljs game"
  :url "http://localhost:3000"

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.10.0"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.6.2"]
                 [http-kit "2.6.0"]
                 [cheshire "5.11.0"]
                 [midje "1.10.9"]]

  :plugins [[lein-midje "3.2.2"]]

  :main sport-quiz.server

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})