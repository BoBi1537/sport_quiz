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

(defn to-engine-question [{:keys [image options answer]}]
  {:prompt (str "What equipment is shown in the image: " image "?")
   :options options
   :answer answer})


(def equipment-game
  (let [prepare-fn prepare-questions
        to-eng to-engine-question
        eval-fn evaluate-answer]
    {:id :equipment
     :title "Sports Equipment Quiz"
     :intro "You will get 5 questions. Each correct answer gives 1 point."
     :prepare-fn prepare-fn
     :to-engine-fn to-eng
     :evaluate-fn eval-fn
     :score-per-question 1}))