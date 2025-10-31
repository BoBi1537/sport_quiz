(ns sport-quiz.db.core
  (:require [migratus.core :as migratus]
            [clojure.tools.logging :as log]))



(def jdbc-url
  "jdbc:mysql://localhost:3306/sportquizdb?serverTimezone=UTC&useSSL=false")

(def db-spec-next-jdbc
  {:jdbcUrl jdbc-url
   :user "root"
   :password "root"})



(def migratus-config
  {:store :database
   :db db-spec-next-jdbc
   :migration-dir "migrations"})



(defn migrate
  "Runs all pending database migrations."
  []
  (log/info "Starting database migrations...")
  (try
    (migratus/migrate migratus-config)
    (log/info "Database migration successful.")
    (catch Exception e
      (log/error e "Database migration failed!")
      (throw e))))

(defn init-db
  "Inicijalizuje bazu i pokreće migracije."
  []
  (migrate))