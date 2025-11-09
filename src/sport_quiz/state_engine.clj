(ns sport-quiz.state-engine
  (:require [sport-quiz.db.queries :as q]
            [sport-quiz.db.core :as db-core]
            [clojure.tools.logging :as log]
            [next.jdbc :as jdbc]
            [cheshire.core :as json]
            [sport-quiz.games.equipment :as eq]
            [sport-quiz.games.athlete :as ath]
            [sport-quiz.games.matching :as mat]))

(def game-specs
  {:equipment eq/equipment-game
   :athlete   ath/athlete-game
   :matching  mat/matching-game})

(defn api-state
  "Return a shape for frontend:
  {:game-id ..., :current-question ..., :score ..., :progress ..., :completed? ..., :index ..., :total ...}"
  [{:keys [game_id current_index question_order score completed?] :as full-state}
   current-question]
  (let [total (count question_order)]
    (log/info "Inside api-state: " full-state)
    {:game-id (keyword game_id)
     :score score
     :completed? completed?
     :progress (if (pos? total) (/ current_index total) 0)
     :current-question current-question
     :index current_index
     :total total}))

(defn start-game-db [game-spec n]
  (jdbc/with-transaction [tx db-core/ds]
    (let [game-id (:id game-spec)
          qids (q/get-random-question-ids tx (name game-id) n)
          score-per-question (:score-per-question game-spec)
          initial-state {:game_id (name game-id)
                         :question_order qids
                         :current_index 0
                         :score 0
                         :completed? false
                         :score_per_question score-per-question}
          session-id (q/create-session! tx "single" initial-state)
          first-qid (first qids)
          raw-question (q/get-question-by-id tx first-qid)
          api-question ((:to-engine-fn game-spec) raw-question)]
      (log/info "Inside start-game-db after let init")
      (log/info "Session-id: " session-id)
      {:session-id session-id
       :state (api-state initial-state api-question)})))

(defn get-state-by-id [session-id]
  (jdbc/with-transaction [tx db-core/ds]
    (when-let [session-record (q/get-session-state tx session-id)]
      (let [data (:game_session/session_data session-record)
            game-key (keyword (:game_id data))
            spec (game-specs game-key)]
        (log/info "Inside get-state-by-id: ")
        (log/info "Data: " data)
        (log/info "Game key: " game-key)
        (log/info "Spec: " spec)
        (assoc data
               :evaluate-fn (:evaluate-fn spec)
               :to-engine-fn (:to-engine-fn spec)
               :session-id session-id)))))

(defn submit-answer-db [session-id user-answer]
  (jdbc/with-transaction [tx db-core/ds]
    (if-let [state (get-state-by-id session-id)]
      (let [{:keys [question_order current_index score score_per_question evaluate-fn completed?]} state
            qid (nth question_order current_index)
            raw-q (q/get-question-by-id tx qid)
            correct? (evaluate-fn raw-q user-answer)
            new-score (if correct? (+ score score_per_question) score)
            last? (= (inc current_index) (count question_order))
            updated-state (-> state
                              (assoc :score new-score)
                              (assoc :current_index (inc current_index))
                              (assoc :completed? last?)
                              (dissoc :evaluate-fn :to-engine-fn))

            _ (q/update-session-state! tx session-id updated-state)
            next-q (when-not last?
                     (q/get-question-by-id tx (nth question_order (inc current_index))))
            next-api-q (when next-q
                         ((:to-engine-fn (game-specs (keyword (:game_id state))))
                          next-q))]
        {:correct? correct?
         :new-state (api-state (assoc updated-state :session-id session-id)
                               next-api-q)
         :raw-question raw-q})
      {:error "Unknown session-id"})))