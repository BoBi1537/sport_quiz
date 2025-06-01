(ns sport-quiz.state-engine-test
  (:require [midje.sweet :refer :all]
            [sport-quiz.state-engine :as se]
            [sport-quiz.games.equipment :as eq]))

(facts "state engine basic flow"
       (fact "start-game creates a valid state"
             (let [st (se/start-game eq/equipment-game 3)]
               (:finished? st) => false
               (:current-index st) => 0
               (count (:questions st)) => 3))

       (fact "current-question returns the first question"
             (let [st (se/start-game eq/equipment-game 3)
                   q (se/current-question st)]
               (map? q) => true
               (:prompt q) =not=> nil?))

       (fact "submit-answer moves to next state"
             (let [st (se/start-game eq/equipment-game 2)
                   q1 (se/current-question st)
                   ans (:answer q1)

                   {:keys [correct? new-state]} (se/submit-answer st ans)]
               correct? => true
               (:current-index new-state) => 1
               (:score new-state) => 1)))