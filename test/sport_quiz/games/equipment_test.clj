(ns sport-quiz.games.equipment-test
  (:require [midje.sweet :refer :all]
            [sport-quiz.games.equipment :as eq]))

(facts "shuffle-question"
       (fact "shuffling options keeps the same elements"
             (let [q (first eq/equipment-questions)
                   shuffled (eq/shuffle-question q)]
               (set (:options q)) => (set (:options shuffled)))))

(facts "prepare-questions"
       (fact "returns all questions"
             (count (eq/prepare-questions)) => (count eq/equipment-questions)))

(facts "evaluate-answer"
       (fact "returns true for correct answer"
             (let [q (first eq/equipment-questions)]
               (eq/evaluate-answer q (:answer q)) => true))

       (fact "returns false for incorrect answer"
             (let [q (first eq/equipment-questions)]
               (eq/evaluate-answer q "Not the answer") => false)))

(facts "to-engine-question"
       (fact "creates engine-compatible question map"
             (let [q (first eq/equipment-questions)
                   e (eq/to-engine-question q)]
               (:prompt e) => truthy
               (:options e) => (:options q)
               (:answer e) => (:answer q))))