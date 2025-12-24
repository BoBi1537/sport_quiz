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
   :athlete  ath/athlete-game
   :matching mat/matching-game})

(def singleplayer-sequence [:equipment :matching :athlete])


(defn- prepare-game-data [tx game-id n]
  (let [spec (game-specs game-id)
        qids (q/get-random-question-ids tx (name game-id) n)
        raw-data (if (= game-id :matching)
                   (mapv #(q/get-question-by-id tx %) qids)
                   (q/get-question-by-id tx (first qids)))]
    {:qids qids
     :raw-data raw-data
     :api-question ((:to-engine-fn spec) raw-data)
     :score-per-question (:score-per-question spec)}))

(defn api-state
  [{:keys [game_id current_index question_order score completed? game-specific-data total_cumulative_score] :as full-state}
   current-question]
  (let [total (count question_order)
        progress-index (if (= (keyword game_id) :matching)
                         (:attempts-count game-specific-data 0)
                         current_index)
        total-q (if (= (keyword game_id) :matching)
                  (:total-pairs game-specific-data 0)
                  total)]
    {:game-id (keyword game_id)
     :score score
     :total_cumulative_score (or total_cumulative_score score)
     :completed? completed?
     :progress (if (pos? total-q) (/ progress-index total-q) 0)
     :current-question current-question
     :index progress-index
     :total total-q}))

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
                                                :total-pairs n
                                                :attempts-count 0})
                         }
          session-id (q/create-session! tx "single" initial-state)]
      (log/info "Inside start-game-db after let init")
      {:session-id session-id
       :state (api-state initial-state api-question)})))

(defn start-full-game-db []
  (jdbc/with-transaction [tx db-core/ds]
    (let [first-game-id (first singleplayer-sequence)
          n (if (= first-game-id :matching) 8 5)
          game-data (prepare-game-data tx first-game-id n)
          initial-state {:game_id (name first-game-id)
                         :game_sequence (map name (rest singleplayer-sequence))
                         :question_order (:qids game-data)
                         :current_index 0
                         :score 0
                         :total_cumulative_score 0
                         :completed? false
                         :score_per-question (:score-per-question game-data)
                         :game-specific-data (when (= first-game-id :matching)
                                               {:raw-questions (:raw-data game-data)
                                                :solved-pairs []
                                                :total-pairs n
                                                :attempts-count 0})}
          session-id (q/create-session! tx "single" initial-state)]
      {:session-id session-id
       :state (api-state initial-state (:api-question game-data))})))

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
      (let [{:keys [game_id question_order current_index score game-specific-data
                    evaluate-fn to-engine-fn game_sequence total_cumulative_score]
             score_per-question :score_per-question} state
            game-key (keyword game_id)
            raw-q (if (= game-key :matching)
                    (:raw-questions game-specific-data)
                    (q/get-question-by-id tx (nth question_order current_index)))
            correct? (if-not (nil? user-answer) (evaluate-fn raw-q user-answer) false)
            new-score (if correct? (+ score score_per-question) score)
            correct-answer-to-return (when (and (not correct?)
                                                (not= game-key :matching))
                                       (:answer raw-q))
            sub-game-finished? (if (= game-key :matching)
                                 (or (nil? user-answer)
                                     (>= (inc (:attempts-count game-specific-data 0)) (:total-pairs game-specific-data)))
                                 (= (inc current_index) (count question_order)))]
        (let [updated-state
              (if sub-game-finished?
                (if (seq game_sequence)
                  (let [next-game-id (keyword (first game_sequence))
                        n (if (= next-game-id :matching) 8 5)
                        next-game-data (prepare-game-data tx next-game-id n)]
                    {:game_id (name next-game-id)
                     :game_sequence (rest game_sequence)
                     :question_order (:qids next-game-data)
                     :current_index 0
                     :score 0
                     :total_cumulative_score (+ total_cumulative_score new-score)
                     :completed? false
                     :score_per-question (:score-per-question next-game-data)
                     :game-specific-data (when (= next-game-id :matching)
                                           {:raw-questions (:raw-data next-game-data)
                                            :solved-pairs []
                                            :total-pairs n
                                            :attempts-count 0})})
                  (assoc state :completed? true
                         :score new-score
                         :total_cumulative_score (+ total_cumulative_score new-score)))
                (if (= game-key :matching)
                  (let [new-attempts (inc (:attempts-count game-specific-data 0))
                        new-solved (if correct? (conj (:solved-pairs game-specific-data) user-answer) (:solved-pairs game-specific-data))]
                    (assoc state :score new-score
                           :game-specific-data (assoc game-specific-data
                                                      :solved-pairs new-solved
                                                      :attempts-count new-attempts)))
                  (assoc state :score new-score :current_index (inc current_index))))]
          (q/update-session-state! tx session-id (dissoc updated-state :evaluate-fn :to-engine-fn :current-question :session-id))
          (let [next-spec (game-specs (keyword (:game_id updated-state)))
                next-q-api (if (:completed? updated-state)
                             nil
                             (if (= (keyword (:game_id updated-state)) :matching)
                               ((:to-engine-fn next-spec) (:raw-questions (:game-specific-data updated-state)))
                               ((:to-engine-fn next-spec) (q/get-question-by-id tx (nth (:question_order updated-state) (:current_index updated-state))))))]
            {:correct? correct?
             :new-state (api-state updated-state next-q-api)
             :correct-answer correct-answer-to-return})))
      {:error "Unknown session-id"})))