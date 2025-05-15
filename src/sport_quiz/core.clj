(ns sport-quiz.core
  (:require [sport-quiz.engine :as engine]
            [sport-quiz.games.equipment :as equipment]
            [sport-quiz.games.matching :as matching]
            [sport-quiz.games.athlete :as athlete]
            [sport-quiz.game :as game]
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
             {:title (:title equipment/equipment-game)
              :intro (:intro equipment/equipment-game)
              :score-per-question (:score-per-question equipment/equipment-game)
              :question-generator (fn []
                                    (game/question-generator equipment/equipment-game 5))
              :answer-fn (fn [q sel]
                           (game/evaluate equipment/equipment-game q sel))})
            (recur))
        2 (do
            (ui/clear-screen)
            (println (:title matching/matching-game))
            (println (:intro matching/matching-game))
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
             {:title (:title athlete/athlete-game)
              :intro (:intro athlete/athlete-game)
              :score-per-question (:score-per-question athlete/athlete-game)
              :question-generator (fn []
                                    (game/question-generator athlete/athlete-game 5))
              :answer-fn (fn [q sel]
                           (game/evaluate athlete/athlete-game q sel))})
            (recur))
        0 (do
            (println "Goodbye!")
            (System/exit 0))
        (do
          (println "Invalid input. Try again.")
          (Thread/sleep 1200)
          (recur))))))