(ns sport-quiz.games.athlete)

(def athlete-questions
  [{:description "Spanish tennis legend with 22 Grand Slam titles."
    :answer "Rafael Nadal"
    :options ["Rafael Nadal" "Roger Federer" "Novak Djokovic" "Andy Murray"]}

   {:description "Argentinian footballer, World Cup champion, plays for Inter Miami."
    :answer "Lionel Messi"
    :options ["Cristiano Ronaldo" "Lionel Messi" "Neymar" "Luis Suarez"]}

   {:description "American basketball player, 4x NBA champion, plays for the Lakers."
    :answer "LeBron James"
    :options ["Kevin Durant" "Stephen Curry" "LeBron James" "Kobe Bryant"]}

   {:description "Swiss tennis player with 20 Grand Slam titles."
    :answer "Roger Federer"
    :options ["Rafael Nadal" "Roger Federer" "Pete Sampras" "Boris Becker"]}

   {:description "Portuguese footballer with over 850 career goals."
    :answer "Cristiano Ronaldo"
    :options ["Lionel Messi" "Cristiano Ronaldo" "Kylian Mbappe" "Erling Haaland"]}

   {:description "Serbian tennis player with 24 Grand Slam titles."
    :answer "Novak Djokovic"
    :options ["Novak Djokovic" "Roger Federer" "Rafael Nadal" "Stan Wawrinka"]}])

(defn shuffle-question [q]
  (update q :options shuffle))

(defn prepare-questions []
  (shuffle athlete-questions))

(defn evaluate-answer [q user-choice]
  (= (:answer q) user-choice))