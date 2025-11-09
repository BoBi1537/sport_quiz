(ns sport-quiz.db.core
  (:require [migratus.core :as migratus]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [sport-quiz.games.equipment :as equipment]
            [sport-quiz.games.athlete :as athlete]
            [sport-quiz.games.matching :as matching]))

(def jdbc-url "jdbc:mysql://localhost:3306/sportquizdb?serverTimezone=UTC&useSSL=false&characterEncoding=utf8")

(def db-spec-next-jdbc
  {:jdbcUrl jdbc-url
   :user "root"
   :password "root"})

(def ds (jdbc/get-datasource db-spec-next-jdbc))
(def migratus-config
  {:store :database
   :db db-spec-next-jdbc
   :migration-dir "migrations"})

(defn migrate []
  (log/info "Starting database migrations...")
  (try
    (migratus/migrate migratus-config)
    (log/info "Database migration successful.")
    (catch Exception e
      (log/error e "Database migration failed!")
      (throw e))))

(def initial-questions
  (concat
   (map #(assoc % :game-id "equipment") equipment/equipment-questions)
   (map #(assoc % :game-id "athlete") athlete/athlete-questions)
   (map (fn [p]
          {:game-id "matching"
           :prompt (:sport p)
           :options (map :equipment matching/matching-pairs)
           :answer (:equipment p)})
        matching/matching-pairs)))

(defn seed-questions
  "Seed questions into the question table if empty.
   Stores question_data as JSON in the question table (column type JSON)."
  []
  (jdbc/with-transaction [tx ds]
    (let [row (jdbc/execute-one! tx ["SELECT COUNT(*) AS c FROM question"])]
      (if (pos? (:c row))
        (log/info "Questions table already populated.")
        (do
          (log/info "Seeding initial questions into DB...")
          (let [batch-data
                (mapv (fn [q]
                        {:game_id (str (:game-id q))
                         :question_data (json/generate-string (dissoc q :game-id))})
                      initial-questions)]
            (log/info (str "Attempting to seed " (count batch-data) " questions."))
            (sql/insert-multi! tx :question batch-data {:batch true})
            (log/info (str "Seeded " (count batch-data) " questions."))))))))

(defn init-db
  "Run migrations and seed questions (idempotent)."
  []
  (migrate)
  (seed-questions))