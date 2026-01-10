(ns sport-quiz.api
  (:require [ajax.core :refer [POST]]
            [ajax.core :as ajax]
            [clojure.string :as str]
            [clojure.core :refer [shuffle]]
            [sport-quiz.state :as state :refer [normalize-game-id get-max-time]]))

(defn stop-timer! []
  (when @state/timer-handle
    (js/clearInterval @state/timer-handle)
    (reset! state/timer-handle nil)))

(declare auto-advance)
(defn start-question-timer! [game-id]
  (stop-timer!)
  (let [current-game-id (normalize-game-id game-id)
        max-time (get-max-time current-game-id)]
    (if max-time
      (do
        (reset! state/time-remaining max-time)
        (reset! state/timer-handle
                (js/setInterval
                 (fn []
                   (swap! state/time-remaining dec)
                   (when (<= @state/time-remaining 0)
                     (stop-timer!)
                     (if (= current-game-id :matching)
                       (do
                         (js/console.log "MATCHING TIME EXPIRED")
                         (reset! state/matching-game-ended-locally true)
                         (auto-advance))
                       (auto-advance))))
                 1000)))
      (reset! state/time-remaining nil))))

(defn reset-matching-atoms! []
  (reset! state/selected-match-item nil)
  (reset! state/prompt-to-answer-map nil)
  (reset! state/shuffled-prompts-state nil)
  (reset! state/shuffled-answers-state nil)
  (reset! state/original-pairs-map nil)
  (reset! state/solved-pairs-local #{})
  (reset! state/wrongly-paired-prompts-local #{})
  (reset! state/matching-game-ended-locally false))

(defn auto-advance []
  (stop-timer!)
  (let [sid @state/session-id
        old-game-state @state/game-state
        current-game-id (normalize-game-id (:game-id old-game-state))
        answer (if (= current-game-id :matching) [] nil)
        delay-time (if (= current-game-id :matching)
                     state/matching-game-end-delay
                     state/classic-quiz-next-question-delay)]
    (POST "/api/answer"
      {:params {:session-id sid :answer answer}
       :format :json
       :response-format (ajax/json-response-format {:keywords? true})
       :handler
       (fn [{:keys [correct-answer state]}]
         (let [new-state state
               new-game-id (normalize-game-id (:game-id new-state))]
           (when-not (= current-game-id :matching)
             (reset! state/last-answer-correct? false)
             (reset! state/correct-answer correct-answer)
             (reset! state/last-chosen-answer "Timeout"))
           (js/setTimeout
            (fn []
              (stop-timer!)
              (reset! state/matching-game-ended-locally false)
              (reset! state/last-answer-correct? nil)
              (reset! state/last-chosen-answer nil)
              (reset! state/correct-answer nil)
              (when (or (:completed? new-state) (not= (:game-id old-game-state) (:game-id new-state)))
                (reset! state/total-score (:total_cumulative_score new-state)))
              (reset! state/game-state new-state)
              (if (:completed? new-state)
                (stop-timer!)
                (do
                  (if (not= new-game-id current-game-id)
                    (do
                      (reset-matching-atoms!)
                      (when (= new-game-id :matching)
                        (let [{:keys [prompts answers]} (:current-question new-state)]
                          (reset! state/original-pairs-map (zipmap prompts answers))
                          (reset! state/prompt-to-answer-map (zipmap prompts answers))
                          (reset! state/shuffled-prompts-state (shuffle prompts))
                          (reset! state/shuffled-answers-state (shuffle answers)))))
                    (when (not= new-game-id :matching)
                      (reset-matching-atoms!)))
                  (start-question-timer! new-game-id))))
            delay-time)))})))


(defn start-full-game []
  (stop-timer!)
  (reset! state/last-answer-correct? nil)
  (reset! state/correct-answer nil)
  (reset! state/last-chosen-answer nil)
  (reset! state/time-remaining nil)
  (reset-matching-atoms!)
  (POST "/api/start-full"
    {:format :json
     :response-format (ajax/json-response-format {:keywords? true})
     :handler (fn [resp]
                (js/console.log "START-FULL: Game sequence started.")
                (reset! state/session-id (:session-id resp))
                (let [l-state (:state resp)
                      game-id-k (normalize-game-id (:game-id l-state))]
                  (when (= game-id-k :matching)
                    (let [{:keys [prompts answers]} (:current-question l-state)]
                      (reset! state/original-pairs-map (zipmap prompts answers))
                      (reset! state/prompt-to-answer-map (zipmap prompts answers))
                      (reset! state/shuffled-prompts-state (shuffle prompts))
                      (reset! state/shuffled-answers-state (shuffle answers))))
                  (reset! state/game-state l-state)
                  (start-question-timer! (:game-id l-state))))
     :error-handler #(js/console.error "Start full error:" %)}))

(defn start-game [game-id n]
  (stop-timer!)
  (reset! state/last-answer-correct? nil)
  (reset! state/correct-answer nil)
  (reset! state/last-chosen-answer nil)
  (reset! state/time-remaining nil)
  (reset-matching-atoms!)
  (POST "/api/start"
    {:params {:game game-id :n n}
     :format :json
     :response-format (ajax/json-response-format {:keywords? true})
     :handler (fn [resp]
                (js/console.log "START-GAME: Initial state loaded." (:state resp))
                (reset! state/session-id (:session-id resp))
                (let [l-state (:state resp)
                      game-id-k (normalize-game-id (:game-id l-state))]
                  (when (= game-id-k :matching)
                    (let [{:keys [prompts answers]} (:current-question l-state)]
                      (reset! state/original-pairs-map (zipmap prompts answers))
                      (reset! state/prompt-to-answer-map (zipmap prompts answers))
                      (reset! state/shuffled-prompts-state (shuffle prompts))
                      (reset! state/shuffled-answers-state (shuffle answers))))
                  (reset! state/game-state l-state)
                  (start-question-timer! (:game-id l-state))))
     :error-handler #(js/console.error "Start error:" %)}))

(defn submit-answer [answer]
  (when-let [sid @state/session-id]
    (let [l-game-state state/game-state
          old-state @l-game-state
          old-game-id (normalize-game-id (:game-id old-state))
          is-matching-pair (and (vector? answer) (= 2 (count answer)))]
      (when-not is-matching-pair (stop-timer!))
      (POST "/api/answer"
        {:params {:session-id sid :answer answer}
         :format :json
         :response-format (ajax/json-response-format {:keywords? true})
         :handler
         (fn [{:keys [correct? correct-answer state]}]
           (let [new-state state
                 new-game-id (normalize-game-id (:game-id new-state))]
             (if is-matching-pair
               (do
                 (reset! state/last-answer-correct? correct?)
                 (reset! state/selected-match-item nil)
                 (when correct? (swap! state/solved-pairs-local conj answer))
                 (when-not correct? (swap! state/wrongly-paired-prompts-local conj (first answer)))
                 (if (or (:completed? new-state) (not= new-game-id old-game-id))
                   (do
                     (stop-timer!)
                     (reset! state/matching-game-ended-locally true)
                     (js/setTimeout
                      (fn []
                        (reset! state/matching-game-ended-locally false)
                        (reset! state/last-answer-correct? nil)
                        (reset! state/last-chosen-answer nil)
                        (reset! state/total-score (:total_cumulative_score new-state))
                        (reset! l-game-state new-state)
                        (when-not (:completed? new-state)
                          (start-question-timer! new-game-id)))
                      state/matching-game-end-delay))
                   (reset! l-game-state new-state)))
               (do
                 (reset! state/last-answer-correct? correct?)
                 (reset! state/correct-answer correct-answer)
                 (reset! state/last-chosen-answer answer)
                 (js/setTimeout
                  (fn []
                    (reset! state/last-answer-correct? nil)
                    (reset! state/last-chosen-answer nil)
                    (reset! state/correct-answer nil)
                    (when (not= new-game-id old-game-id)
                      (reset! state/total-score (:total_cumulative_score new-state))
                      (reset-matching-atoms!)
                      (when (= new-game-id :matching)
                        (let [{:keys [prompts answers]} (:current-question new-state)]
                          (reset! state/original-pairs-map (zipmap prompts answers))
                          (reset! state/shuffled-prompts-state (shuffle prompts))
                          (reset! state/shuffled-answers-state (shuffle answers)))))
                    (reset! l-game-state new-state)
                    (if (:completed? new-state)
                      (stop-timer!)
                      (start-question-timer! new-game-id)))
                  state/classic-quiz-next-question-delay)))))
         :error-handler #(js/console.error "Submit error:" %)}))))