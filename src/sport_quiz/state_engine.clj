(ns sport-quiz.state-engine)

(defn start-game [game n]
  (let [raw-q (take n ((:prepare-fn game)))
        engine-q (map (:to-engine-fn game) raw-q)]
    {:game-id (:id game)
     :raw-questions (vec raw-q)
     :questions (vec engine-q)
     :current-index 0
     :score 0
     :completed? false
     :score-per-question (:score-per-question game)
     :evaluate-fn (:evaluate-fn game)}))

(defn submit-answer
  "Evaluates answer, moves to next question."
  [{:keys [current-index raw-questions score score-per-question] :as state} user-answer]
  (let [current-raw (nth raw-questions current-index)
        correct? ((:evaluate-fn state) current-raw user-answer)
        new-score (if correct?
                    (+ score score-per-question)
                    score)
        last? (= (inc current-index) (count raw-questions))
        new-state (assoc state
                         :current-index (inc current-index)
                         :score new-score
                         :completed? last?)]
    {:correct? correct?
     :new-state new-state}))

(defn api-state
  "Return a frontend-friendly view of the current state."
  [{:keys [questions current-index score completed?]}]
  (let [q (nth questions current-index nil)
        total (count questions)]
    {:current-question q
     :score score
     :progress (if (pos? total) (/ current-index total) 0)
     :completed? completed?}))