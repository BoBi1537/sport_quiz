(ns sport-quiz.game-test
  (:require [midje.sweet :refer :all]
            [sport-quiz.game :as g]
            [sport-quiz.games.equipment :as eq]))

(facts "game/question-generator"
       (fact "generates engine questions count"
             (let [qs (g/question-generator eq/equipment-game 3)]
               (count qs) => 3
               (every? #(and (:prompt %) (:options %) (:answer %)) qs) => true))

       (fact "evaluate delegates to game's evaluate-fn"
             (let [q (first (eq/prepare-questions))]
               (g/evaluate eq/equipment-game q (:answer q)) => true)))