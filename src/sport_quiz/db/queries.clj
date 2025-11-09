(ns sport-quiz.db.queries
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [cheshire.core :as json]
            [sport-quiz.db.core :as db]
            [clojure.tools.logging :as log]))



(defn get-random-question-ids
  "Return vector of random question IDs for a given game, limit n."
  [tx game-id n]
  (vec (map :question/id
            (jdbc/execute!
             tx
             ["SELECT id FROM question WHERE game_id = ? ORDER BY RAND() LIMIT ?" game-id n]))))

(defn get-question-by-id
  "Return parsed question_data for a given question ID."
  [tx qid]
  (when-let [row (jdbc/execute-one! tx ["SELECT question_data FROM question WHERE id = ?" qid])]
    (json/parse-string-strict (:question/question_data row) true)))



(defn create-session!
  "Create a game_session row with minimal normalized session_data.
  session_data structure:
  {:game_id ..., :question_order [...], :current_index 0, :score 0, :completed? false, :score_per_question n}"
  [tx mode session-data]
  (let [session-json (json/generate-string session-data)]
    (log/info "Mode: " mode)
    (log/info "Session_data" session-json)
    (sql/insert! tx :game_session
                 {:mode mode
                  :status "active"
                  :session_data session-json})
    (log/info "Successfully executed insert query")
    (-> (sql/query tx ["SELECT LAST_INSERT_ID() AS id"])
        first
        :id)))

(defn get-session-state
  "Load session row and parse session_data JSON."
  [tx session-id]
  (log/info "Unutar get-session-state")
  (log/info "Session-id: " session-id)
  (when-let [row (sql/get-by-id tx :game_session session-id)]
    (log/info "Row: " row)
    (let [data (json/parse-string-strict (:game_session/session_data row) true)]
      (log/info "Data: " data)
      (assoc row :game_session/session_data data))))

(defn update-session-state!
  "Write updated Clojure state back to DB as JSON."
  [tx session-id new-state]
  (sql/update! tx :game_session
               {:session_data (json/generate-string new-state)}
               {:id session-id}))


(defn create-player-session! [tx game-session-id player-uuid is-host?]
  (sql/insert! tx :player_session
               {:game_session_id game-session-id
                :player_uuid player-uuid
                :is_host is-host?}))