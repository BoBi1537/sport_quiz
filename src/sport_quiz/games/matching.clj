(ns sport-quiz.games.matching)

(def matching-pairs
  [{:sport "Tennis" :equipment "Racket"}
   {:sport "Football" :equipment "Ball"}
   {:sport "Basketball" :equipment "Hoop"}
   {:sport "Boxing" :equipment "Gloves"}
   {:sport "Baseball" :equipment "Bat"}
   {:sport "Hockey" :equipment "Stick"}])

(defn prepare-pairs []
  (shuffle matching-pairs))

(defn evaluate-match [pair choice]
  (= (:equipment pair) choice))

(defn to-engine-question [{:keys [sport equipment]}]
  {:prompt (str "Which equipment belongs to " sport "?")
   :options ["Racket" "Ball" "Hoop" "Gloves" "Bat" "Stick"]})

(def matching-game
  {:id :matching
   :title "Matching Game"
   :intro "Match sport with correct equipment."
   :prepare-fn prepare-pairs
   :to-engine-fn to-engine-question
   :evaluate-fn evaluate-match
   :score-per-question 2})