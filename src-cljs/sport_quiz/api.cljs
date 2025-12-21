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
  (let [max-time (get-max-time game-id)
        current-game-id (normalize-game-id game-id)]
    (if max-time
      (let [tr state/time-remaining
            th state/timer-handle]
        (reset! tr max-time)
        (reset! th
                (js/setInterval
                 (fn []
                   (swap! tr dec)
                   (when (<= @tr 0)
                     (js/clearInterval @th)
                     (reset! th nil)
                     (if (= current-game-id :matching)
                       (do
                         (js/console.log "MATCHING GAME: Time expired, auto-advance for game end.")
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
        l-game-state state/game-state
        current-game-id (normalize-game-id (:game-id @l-game-state))
        answer (if (= current-game-id :matching) [] nil)
        delay-time (if (= current-game-id :matching) state/matching-game-end-delay state/classic-quiz-next-question-delay)]
    (js/console.log "AUTO-ADVANCE: Sending null answer for timeout.")
    (POST "/api/answer"
      {:params {:session-id sid :answer answer}
       :format :json
       :response-format (ajax/json-response-format {:keywords? true})
       :handler
       (fn [{:keys [correct-answer state] :as resp}]
         (let [new-state state
               new-game-id (normalize-game-id (:game-id new-state))]
           (js/console.log "AUTO-ADVANCE: Server response received (Timeout). Completed?:" (:completed? new-state))
           (when-not (= current-game-id :matching)
             (reset! state/last-answer-correct? false)
             (reset! state/correct-answer correct-answer)
             (reset! state/last-chosen-answer "Timeout"))
           (if (and (= current-game-id :matching) (:completed? new-state))
             (do
               (js/console.log "MATCHING GAME END (Timeout): Showing sorted pairs for" delay-time "ms.")
               (js/setTimeout
                (fn []
                  (js/console.log "MATCHING GAME END: Transitioning to Final Score view.")
                  (reset! state/matching-game-ended-locally false)
                  (reset! l-game-state new-state))
                delay-time))
             (js/setTimeout
              (fn []
                (js/console.log "AUTO-ADVANCE: Timer expired. Attempting cleanup and transition.")
                (reset! l-game-state new-state)
                (if (:completed? new-state)
                  (do
                    (js/console.log "AUTO-ADVANCE: Quiz completed.")
                    (stop-timer!))
                  (if (and new-state (:current-question new-state))
                    (do
                      (reset! state/last-answer-correct? nil)
                      (reset! state/last-chosen-answer nil)
                      (reset! state/correct-answer nil)
                      (when-not (= new-game-id :matching)
                        (reset-matching-atoms!))
                      (when (= new-game-id :matching)
                        (let [{:keys [prompts answers]} (:current-question new-state)]
                          (reset! state/prompt-to-answer-map (zipmap prompts answers))
                          (reset! state/shuffled-prompts-state (shuffle prompts))
                          (reset! state/shuffled-answers-state (shuffle answers))))
                      (start-question-timer! (:game-id new-state)))
                    (js/console.error "AUTO-ADVANCE: ERROR! New state is invalid or missing current-question." new-state))))
              delay-time)))
         :error-handler #(js/console.error "Timeout error:" %))})))


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
    (let [is-matching-pair (and (vector? answer) (= 2 (count answer)))
          l-game-state state/game-state
          game-id (:game-id @l-game-state)]
      (when-not is-matching-pair
        (stop-timer!))
      (if is-matching-pair
        (do
          (reset! state/last-chosen-answer answer)
          (js/console.log "SUBMIT-MATCH: Submitting pair:" answer)
          (POST "/api/answer"
            {:params {:session-id sid :answer answer}
             :format :json
             :response-format (ajax/json-response-format {:keywords? true})
             :handler
             (fn [{:keys [correct? state] :as resp}]
               (let [new-state state]
                 (reset! state/last-answer-correct? correct?)
                 (reset! state/selected-match-item nil)
                 (reset! state/last-chosen-answer nil)
                 (if correct?
                   (do
                     (js/console.log "MATCH: Correct pair found. Storing locally.")
                     (swap! state/solved-pairs-local conj answer)
                     (if (:completed? new-state)
                       (do
                         (stop-timer!)
                         (reset! state/game-state new-state)
                         (reset! state/matching-game-ended-locally true)
                         (js/console.log "MATCHING GAME COMPLETED: Showing sorted pairs for" state/matching-game-end-delay "ms.")
                         (js/setTimeout
                          (fn []
                            (js/console.log "MATCHING GAME END: Transitioning to Final Score view.")
                            (reset! state/matching-game-ended-locally false)
                            (reset! l-game-state new-state))
                          state/matching-game-end-delay))
                       (reset! l-game-state new-state)))
                   (do
                     (swap! state/wrongly-paired-prompts-local conj (first answer))
                     (reset! l-game-state new-state)
                     (when (:completed? new-state)
                       (stop-timer!)
                       (reset! state/matching-game-ended-locally true)
                       (js/console.log "MATCHING GAME COMPLETED (on Wrong Answer): Showing sorted pairs.")
                       (js/setTimeout
                        (fn []
                          (reset! state/matching-game-ended-locally false)
                          (reset! l-game-state new-state))
                        state/matching-game-end-delay))))))
             :error-handler #(js/console.error "Match error:" %)}))
        (do
          (reset! state/last-chosen-answer answer)
          (let [l-correct-answer state/correct-answer]
            (POST "/api/answer"
              {:params {:session-id sid :answer answer}
               :format :json
               :response-format (ajax/json-response-format {:keywords? true})
               :handler
               (fn [{:keys [correct? correct-answer state] :as resp}]
                 (let [new-state state]
                   (reset! state/last-answer-correct? correct?)
                   (reset! l-correct-answer (if correct? answer correct-answer))
                   (reset! state/time-remaining nil)
                   (if-not (:completed? new-state)
                     (js/setTimeout
                      (fn []
                        (reset! l-game-state new-state)
                        (reset! state/last-answer-correct? nil)
                        (reset! state/last-chosen-answer nil)
                        (reset! state/correct-answer nil)
                        (start-question-timer! game-id))
                      state/classic-quiz-next-question-delay)
                     (js/setTimeout
                      #(reset! l-game-state new-state)
                      state/classic-quiz-next-question-delay))))
               :error-handler #(js/console.error "Submit error:" %)})))))))