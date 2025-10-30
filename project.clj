(defproject sport-quiz "0.1.0-SNAPSHOT"
  :description "Sport Quiz - Clojure web + cljs game"
  :url "http://localhost:3000"

  :dependencies [[org.clojure/clojure "1.12.3"]
                 [org.clojure/clojurescript "1.11.60"]
                 [ring/ring-core "1.15.3"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.7.2"]
                 [http-kit "2.8.1"]
                 [cheshire "5.11.0"]
                 [mysql/mysql-connector-java "8.0.33"]
                 [migratus "1.6.4"]]

  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "4.0.1"]]}
             :test {:dependencies [[midje "1.10.10"]]}}

  :plugins [[lein-midje "3.2.2"]]


  :main sport-quiz.server

  :target-path "target/%s"

  :uberjar {:aot :all})