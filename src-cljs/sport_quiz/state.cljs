(ns sport-quiz.state
  (:require [reagent.core :as r]))

(defonce app-root (r/atom nil))
(defonce game-state (r/atom nil))
(defonce last-answer-correct? (r/atom nil))
(defonce correct-answer (r/atom nil))
(defonce last-chosen-answer (r/atom nil))
(defonce session-id (r/atom nil))
(defonce time-remaining (r/atom nil))
(defonce timer-handle (r/atom nil))
(defonce selected-match-item (r/atom nil))
(defonce prompt-to-answer-map (r/atom nil))
(defonce shuffled-prompts-state (r/atom nil))
(defonce shuffled-answers-state (r/atom nil))
(defonce original-pairs-map (r/atom nil))
(defonce matching-game-ended-locally (r/atom false))
(defonce solved-pairs-local (r/atom #{}))
(defonce wrongly-paired-prompts-local (r/atom #{}))

(def max-time-equipment 10)
(def max-time-athlete 15)
(def max-time-matching 60)
(def classic-quiz-next-question-delay 1500)
(def matching-game-end-delay 5000)

(defn normalize-game-id [gid]
  (cond
    (keyword? gid) gid
    (string? gid) (keyword gid)
    :else gid))

(defn get-max-time [game-id]
  (case (normalize-game-id game-id)
    :equipment max-time-equipment
    :athlete max-time-athlete
    :matching max-time-matching
    nil))