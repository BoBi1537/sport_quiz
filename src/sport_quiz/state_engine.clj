(ns sport-quiz.state-engine
  (:require [sport-quiz.game :as game]))

(defn start-game
  "Creates an initial game state for N questions."
  [game-descriptor n]
  {:game game-descriptor
   :questions (vec (game/question-generator game-descriptor n))
   :current-index 0
   :score 0
   :finished? false})

(defn current-question
  "Returns the question at the current index."
  [state]
  (get (:questions state) (:current-index state)))

(defn game-over?
  [state]
  (:finished? state))

(defn submit-answer
  "Evaluates answer for the current question.
   Returns {:correct? bool :new-state <updated-state>}."
  [state user-answer]
  (let [game-desc (:game state)
        q (current-question state)
        correct? (game/evaluate game-desc q user-answer)
        new-score (if correct? (inc (:score state)) (:score state))
        last? (= (inc (:current-index state)) (count (:questions state)))
        new-state (-> state
                      (assoc :score new-score)
                      (update :current-index inc)
                      (assoc :finished? last?))]
    {:correct? correct?
     :new-state new-state}))