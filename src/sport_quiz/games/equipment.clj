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

(defn shuffle-question
  "Returns a question with shuffled options."
  [question]
  (update question :options shuffle))

(defn prepare-questions
  "Shuffles all questions and their options."
  []
  (map shuffle-question (shuffle equipment-questions)))

(defn evaluate-answer
  "Returns true if the chosen option matches the correct answer."
  [question chosen]
  (= chosen (:answer question)))