(ns sport-quiz.games.matching)

(def matching-pairs
  [{:sport "Tennis" :equipment "Racket"}
   {:sport "Football" :equipment "Ball"}
   {:sport "Basketball" :equipment "Hoop"}
   {:sport "Boxing" :equipment "Gloves"}
   {:sport "Baseball" :equipment "Bat"}
   {:sport "Hockey" :equipment "Stick"}])

(defn shuffle-pairs []
  (shuffle matching-pairs))

(defn evaluate-match [pair user-choice]
  (= (:equipment pair) user-choice))