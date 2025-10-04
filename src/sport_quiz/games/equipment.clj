(ns sport-quiz.games.equipment)

(def equipment-questions
  [{:image "ball.png"
    :options ["Basketball" "Football" "Tennis Ball" "Baseball"]
    :answer "Basketball"}

   {:image "racket.png"
    :options ["Badminton Racket" "Tennis Racket" "Squash Racket" "Ping Pong Paddle"]
    :answer "Tennis Racket"}

   {:image "helmet.png"
    :options ["Baseball Helmet" "Hockey Helmet" "Football Helmet" "Cycling Helmet"]
    :answer "Hockey Helmet"}

   {:image "skates.png"
    :options ["Figure Skates" "Hockey Skates" "Speed Skates" "Roller Skates"]
    :answer "Hockey Skates"}

   {:image "bat.png"
    :options ["Baseball Bat" "Cricket Bat" "Golf Club" "Hockey Stick"]
    :answer "Baseball Bat"}])

(defn shuffle-question [question]
  (update question :options shuffle))

(defn prepare-questions []
  (map shuffle-question (shuffle equipment-questions)))

(defn evaluate-answer [question chosen]
  (= chosen (:answer question)))

(defn to-engine-question [{:keys [image options]}]
  {:prompt (str "Which equipment is shown in image: " image "?")
   :options options})

(def equipment-game
  {:id :equipment
   :title "Sports Equipment Quiz"
   :intro "Guess the equipment based on the picture."
   :prepare-fn prepare-questions
   :to-engine-fn to-engine-question
   :evaluate-fn evaluate-answer
   :score-per-question 1})