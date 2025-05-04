(ns sport-quiz.core
  (:require [sport-quiz.engine :as engine]
            [sport-quiz.games.equipment :as equipment]
            [sport-quiz.games.matching :as matching]
            [sport-quiz.games.athlete :as athlete]
            [sport-quiz.ui :as ui])
  (:gen-class))

(defn print-menu []
  (ui/clear-screen)
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
    (let [choice (ui/read-int)]
      (case choice
        1 (do
            (engine/run-quiz-game
             {:title "Sports Equipment Quiz"
              :intro "You will get 5 questions. Each correct answer gives 1 point."
              :score-per-question 1
              :question-generator (fn []
                                    (->> (equipment/prepare-questions)
                                         (take 5)
                                         (map equipment/to-engine-question)))
              :answer-fn equipment/evaluate-answer})
            (recur))
        2 (do
            (ui/clear-screen)
            (println "Matching Game")
            (println "Match the sport with the correct equipment.")
            (println "You will get 5 pairs.")
            (println "Press ENTER to begin.")
            (read-line)
            (let [pairs (take 5 (matching/shuffle-pairs))]
              (loop [ps pairs
                     score 0]
                (if (empty? ps)
                  (do
                    (ui/clear-screen)
                    (println "Game finished!")
                    (println "Your score:" score "/ 5")
                    (println "Press ENTER to return to the menu.")
                    (read-line))
                  (let [{:keys [sport equipment] :as pair} (first ps)
                        wrong-options (->> ps
                                           (map :equipment)
                                           (remove #(= % equipment))
                                           shuffle
                                           (take 2))
                        displayed (shuffle (conj wrong-options equipment))]
                    (ui/clear-screen)
                    (println "Sport:" sport)
                    (println "Choose the correct equipment:")
                    (doseq [[idx opt] (map-indexed vector displayed)]
                      (println (str (inc idx) ". " opt)))
                    (print "Your choice: ")
                    (flush)
                    (let [choice (ui/read-int)
                          selected (get displayed (dec choice))
                          correct? (matching/evaluate-match pair selected)]
                      (if correct?
                        (println "Correct!")
                        (println "Wrong! Correct answer:" equipment))
                      (Thread/sleep 1200)
                      (recur (rest ps)
                             (if correct? (inc score) score)))))))
            (recur))
        3 (do
            (engine/run-quiz-game
             {:title "Guess The Athlete"
              :intro "You will get 5 questions."
              :score-per-question 2
              :question-generator (fn []
                                    (->> (athlete/prepare-questions)
                                         (take 5)
                                         (map athlete/to-engine-question)))
              :answer-fn athlete/evaluate-answer})
            (recur))
        0 (do
            (println "Goodbye!")
            (System/exit 0))
        (do
          (println "Invalid input. Try again.")
          (Thread/sleep 1200)
          (recur))))))