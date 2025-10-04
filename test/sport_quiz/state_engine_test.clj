(ns sport-quiz.state-engine-test
  (:require [midje.sweet :refer :all]
            [sport-quiz.state-engine :as se]
            [sport-quiz.games.athlete :as ath]))

(fact "Game starts correctly"
      (let [st (se/start-game ath/athlete-game 2)]
        (:game-id st) => :athlete
        (:completed? st) => false
        (count (:questions st)) => 2))

(fact "Answering works"
      (let [st (se/start-game ath/athlete-game 1)
            raw (first (:raw-questions st))
            answer (:answer raw)
            {:keys [correct? new-state]} (se/submit-answer st answer)]
        correct? => true
        (:score new-state) => 1
        (:completed? new-state) => true))