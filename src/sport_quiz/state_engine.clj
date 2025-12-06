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
          [raw-data api-question] (if (= game-id :matching)
                                    (let [raw-list (mapv #(q/get-question-by-id tx %) qids)]
                                      [raw-list
                                       ((:to-engine-fn game-spec) raw-list)])
                                    (let [raw-q (q/get-question-by-id tx (first qids))]
                                      [raw-q
                                       ((:to-engine-fn game-spec) raw-q)]))
          initial-state {:game_id (name game-id)
                         :question_order qids
                         :current_index 0
                         :score 0
                         :completed? false
                         :score_per-question score-per-question
                         :game-specific-data (when (= game-id :matching)
                                               {:raw-questions raw-data
                                                :solved-pairs []
                                                :total-pairs n})}
          session-id (q/create-session! tx "single" initial-state)]
      (log/info "Inside start-game-db after let init")
      {:session-id session-id
       :state (api-state initial-state api-question)})))

(defn get-state-by-id [session-id]
  (jdbc/with-transaction [tx db-core/ds]
    (when-let [session-record (q/get-session-state tx session-id)]
      (let [data (:game_session/session_data session-record)
            game-key (keyword (:game_id data))
            spec (game-specs game-key)]
        (let [current-question (if (= game-key :matching)
                                 ((:to-engine-fn spec) (:raw-questions (:game-specific-data data)))
                                 (when-not (:completed? data)
                                   (let [qid (nth (:question_order data) (:current_index data))
                                         raw-q (q/get-question-by-id tx qid)]
                                     ((:to-engine-fn spec) raw-q))))]
          (assoc data
                 :evaluate-fn (:evaluate-fn spec)
                 :to-engine-fn (:to-engine-fn spec)
                 :current-question current-question
                 :session-id session-id))))))

(defn submit-answer-db [session-id user-answer]
  (jdbc/with-transaction [tx db-core/ds]
    (if-let [state (get-state-by-id session-id)]
      (let [{:keys [game_id question_order current_index score game-specific-data evaluate-fn to-engine-fn]
             score_per-question :score_per-question
             }state
            game-key (keyword game_id)
            [raw-q next-q] (if (= game-key :matching)
                             [(:raw-questions game-specific-data) nil]
                             [(q/get-question-by-id tx (nth question_order current_index))
                              (when (< (inc current_index) (count question_order))
                                (q/get-question-by-id tx (nth question_order (inc current_index))))])
            correct? (if (not (nil? user-answer))
                       (evaluate-fn raw-q user-answer)
                       false)
            updated-state (if (= game-key :matching)
                            (let [solved-pairs (:solved-pairs game-specific-data)
                                  total-pairs (:total-pairs game-specific-data)
                                  new-solved-pairs (if correct? (conj solved-pairs user-answer) solved-pairs)
                                  new-score (if correct? (+ score score_per-question) score)
                                  is-complete (>= (count new-solved-pairs) total-pairs)
                                  updated-gsp (assoc game-specific-data :solved-pairs new-solved-pairs)]
                              (-> state
                                  (assoc :score new-score)
                                  (assoc :completed? is-complete)
                                  (assoc :game-specific-data updated-gsp)
                                  (dissoc :evaluate-fn :to-engine-fn :current-question :session-id)))
                            (let [new-score (if correct? (+ score score_per-question) score)
                                  last? (= (inc current_index) (count question_order))]
                              (-> state
                                  (assoc :score new-score)
                                  (assoc :current_index (inc current_index))
                                  (assoc :completed? last?)
                                  (dissoc :evaluate-fn :to-engine-fn :current-question :session-id))))

            _ (q/update-session-state! tx session-id updated-state)
            next-api-q (if (:completed? updated-state)
                         nil
                         (if (= game-key :matching)
                           (to-engine-fn (:raw-questions (:game-specific-data updated-state)))
                           ((:to-engine-fn (game-specs game-key)) next-q)))
            correct-answer-for-display (if correct?
                                         user-answer
                                         (:answer raw-q))]

        (log/info "Submit Answer: Game ID" game-key ", Correct?" correct? ", Completed?" (:completed? updated-state))
        {:correct? correct?
         :new-state (api-state updated-state next-api-q)
         :raw-question raw-q
         :correct-answer correct-answer-for-display})

      {:error "Unknown session-id"})))