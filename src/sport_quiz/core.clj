(ns sport-quiz.core
  (:gen-class))

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
  (println "Sports Equipment Game")
  (println "Press ENTER to return to the menu.")
  (read-line))

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