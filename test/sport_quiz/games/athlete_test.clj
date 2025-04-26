(ns sport-quiz.games.athlete-test
  (:require [midje.sweet :refer :all]
            [sport-quiz.games.athlete :as a]))

(facts "prepare-questions"
       (fact "returns all questions"
             (count (a/prepare-questions)) => (count a/athlete-questions)))

(facts "shuffle-question"
       (fact "keeps same elements in options"
             (let [q (first a/athlete-questions)
                   sh (a/shuffle-question q)]
               (set (:options q)) => (set (:options sh)))))

(facts "evaluate-answer"
       (fact "correct answer returns true"
             (let [q (first a/athlete-questions)]
               (a/evaluate-answer q (:answer q)) => true))

       (fact "incorrect answer returns false"
             (let [q (first a/athlete-questions)]
               (a/evaluate-answer q "Wrong Person") => false)))