(ns sport-quiz.engine
  (:require [sport-quiz.ui :as ui]))

(defn run-quiz-game
  [{:keys [title intro question-generator answer-fn score-per-question]}]
  (ui/clear-screen)
  (println title)
  (println intro)
  (println "Press ENTER to begin.")
  (read-line)
  (let [questions (question-generator)]
    (loop [qs questions
           score 0]
      (if (empty? qs)
        (do
          (ui/clear-screen)
          (println "Game finished!")
          (println "Your score:" score "/" (* score-per-question (count questions)))
          (println "Press ENTER to return to the menu.")
          (read-line))
        (let [q (first qs)
              options (:options q)]
          (ui/clear-screen)
          (println (:prompt q))
          (println)
          (doseq [[idx opt] (map-indexed vector options)]
            (println (str (inc idx) ". " opt)))
          (print "Your choice: ")
          (flush)
          (let [choice (ui/read-int)
                selected (get options (dec choice))
                correct? (answer-fn q selected)]
            (if correct?
              (println "Correct!")
              (println "Wrong! Correct answer:" (:answer q)))
            (Thread/sleep 1200)
            (recur (rest qs)
                   (if correct?
                     (+ score score-per-question)
                     score))))))))