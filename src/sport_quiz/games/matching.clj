(ns sport-quiz.games.matching
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [clojure.walk :refer [keywordize-keys]]))

(def matching-pairs
  [{:prompt "Alpine Skiing" :answer "Ski"}
   {:prompt "Ice Hockey" :answer "Puck"}
   {:prompt "Cricket" :answer "Wicket"}
   {:prompt "Athletics (Throw)" :answer "Javelin"}
   {:prompt "Canoeing" :answer "Paddle"}
   {:prompt "Cycling" :answer "Helmet"}

   {:prompt "Surfing" :answer "Board"}
   {:prompt "Baseball" :answer "Glove"}
   {:prompt "Football" :answer "Cleats"}
   {:prompt "Fencing" :answer "Mask"}
   {:prompt "Polo" :answer "Mallet"}
   {:prompt "Badminton" :answer "Shuttlecock"}

   {:prompt "Golf" :answer "Club"}
   {:prompt "Swimming" :answer "Goggles"}
   {:prompt "Volleyball" :answer "Net"}
   {:prompt "Billiards" :answer "Cue"}
   {:prompt "Curling" :answer "Rink"}
   {:prompt "Wrestling" :answer "Mat"}])

(defn to-engine-question [raw-questions]
 "Gets a list of raw question maps with :prompt and :answer keys.
  Transforms them into the format needed for the Matching Game engine."
  (log/info "Inside to-engine-question in matching.clj. Raw questions count:" (count raw-questions))

  (let [prompts (mapv :prompt raw-questions)
        answers (mapv :answer raw-questions)
        all-items (shuffle (concat prompts answers))]

    (log/info "All matching items (shuffled):" all-items)
    {:prompt "Match the sport with the correct equipment or location."
     :all-items all-items
     :prompts prompts
     :answers answers
     :round-size (count prompts)}))

(defn evaluate-answer [question chosen-pair]
  "Question is original raw question from the database.
  Chosen-pair is a pair [item1 item2] that the user selected.
  We must check if one item is a PROMPT and the other is an ANSWER, and if they match."


  (if-let [pair (seq chosen-pair)]
    (let [[item1 item2] pair
          prompts (set (map :prompt question))
          answers (set (map :answer question))]

      (log/info "Matching: Evaluating pair" pair)
      (log/info "Matching: Prompts (Set):" prompts)
      (log/info "Matching: Answers (Set):" answers)

      (cond
        (and (contains? prompts item1) (contains? answers item2))
        (some #(and (= item1 (:prompt %)) (= item2 (:answer %))) question)

        (and (contains? prompts item2) (contains? answers item1))
        (some #(and (= item2 (:prompt %)) (= item1 (:answer %))) question)

        :else
        (do (log/warn "Matching: Chosen items do not form a valid Prompt/Answer combination.") false)))
    false))


(def matching-game
  {:id :matching
   :title "Matching Game: Sport & Equipment"
   :intro "Find the correct pair between the sport (or context) and the equipment (or place)."
   :to-engine-fn to-engine-question
   :evaluate-fn evaluate-answer
   :score-per-question 1})