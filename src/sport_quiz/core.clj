(ns sport-quiz.core
  (:gen-class)
  (:require [sport-quiz.games.equipment :as eq]))

(defn read-int []
  (try
    (Integer/parseInt (read-line))
    (catch Exception _ -1)))

(defn clear-screen []
  (print "\u001b[2J")
  (print "\u001b[H")
  (flush))

(defn game-sports-equipment []
  (clear-screen)

  (println "Sports Equipment Quiz")
  (println "You will get 5 questions. Each correct answer gives 1 point.")
  (println "Press ENTER to begin.")
  (read-line)

  (let [questions (take 5 (eq/prepare-questions))]
    (loop [qs questions
           score 0]
      (if (empty? qs)
        (do
          (clear-screen)
          (println "Quiz finished!")
          (println "Your score:" score "/ 5")
          (println "Press ENTER to return to the menu.")
          (read-line))

        (let [{:keys [image options answer] :as q} (first qs)]
          (clear-screen)
          (println "Image:" image) 
          (println "Choose the correct equipment:")
          (doseq [[idx opt] (map-indexed vector options)]
            (println (str (inc idx) ". " opt)))

          (print "Your choice: ")
          (flush)

          (let [user-choice (read-int)
                selected    (get options (dec user-choice))
                correct?    (eq/evaluate-answer q selected)]

            (if correct?
              (println "Correct!")
              (println "Wrong! Correct answer was:" answer))

            (Thread/sleep 1200)

            (recur (rest qs)
                   (if correct? (inc score) score))))))))


(defn game-matching []
  (clear-screen)
  (println "Matching Game")
  (println "Press ENTER to return to the menu.")
  (read-line))

(defn game-guess-athlete []
  (clear-screen)
  (println "Guess the Athlete")
  (println "Press ENTER to return to the menu.")
  (read-line))

(defn print-menu []
  (clear-screen)
  (println "==============================")
  (println "         SPORT QUIZ")
  (println "==============================")
  (println "1. Sports Equipment Game")
  (println "2. Matching Game")
  (println "3. Guess the Athlete")
  (println "0. Exit")
  (println "------------------------------")
  (print "Choose an option: ")
  (flush))

(defn -main [& _]
  (loop []
    (print-menu)
    (let [choice (read-int)]
      (case choice
        1 (do (game-sports-equipment) (recur))
        2 (do (game-matching) (recur))
        3 (do (game-guess-athlete) (recur))
        0 (do (println "Goodbye!") (System/exit 0))
        (do (println "Invalid input. Try again.")
            (Thread/sleep 1200)
            (recur))))))