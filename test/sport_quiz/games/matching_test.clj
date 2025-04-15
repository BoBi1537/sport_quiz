(ns sport-quiz.games.matching-test
  (:require [midje.sweet :refer :all]
            [sport-quiz.games.matching :as m]))

(facts "shuffle-pairs"
       (fact "keeps the same elements"
             (set (m/shuffle-pairs)) => (set m/matching-pairs)))

(facts "evaluate-match"
       (fact "returns true on correct equipment"
             (let [p {:sport "Tennis" :equipment "Racket"}]
               (m/evaluate-match p "Racket") => true))

       (fact "returns false on incorrect equipment"
             (let [p {:sport "Tennis" :equipment "Racket"}]
               (m/evaluate-match p "Gloves") => false)))