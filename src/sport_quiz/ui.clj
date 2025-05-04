(ns sport-quiz.ui)

(defn clear-screen []
  (print "\u001b[2J")
  (print "\u001b[H")
  (flush))

(defn read-int []
  (try
    (Integer/parseInt (read-line))
    (catch Exception _ -1)))