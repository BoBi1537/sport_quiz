(ns sport-quiz.game)

(defn make-game
  "Create a game descriptor map.
   keys:
     :id - keyword id
     :title - title
     :intro - intro text
     :prepare-fn - zero-arg fn returning seq of raw questions
     :to-engine-fn - fn that converts raw question -> engine question map
     :evaluate-fn - fn that evaluates (raw-or-engine-question, selected) -> boolean
     :score-per-question - integer"
  [{:keys [id title intro prepare-fn to-engine-fn evaluate-fn score-per-question]}]
  {:id id
   :title title
   :intro intro
   :prepare-fn prepare-fn
   :to-engine-fn to-engine-fn
   :evaluate-fn evaluate-fn
   :score-per-question (or score-per-question 1)})

(defn question-generator
  "Generate n engine-compatible questions from a game descriptor.
   Returns a seq of {:prompt ... :options [...] :answer ...} maps."
  ([game] (question-generator game Long/MAX_VALUE))
  ([game n]
   (->> ((:prepare-fn game))
        (take n)
        (map (:to-engine-fn game)))))

(defn evaluate
  "Evaluate an answer for a game descriptor.
   Accepts an engine question (map) or raw question as produced by prepare-fn.
   Delegates to game's evaluate-fn."
  [game q selected]
  ((:evaluate-fn game) q selected))