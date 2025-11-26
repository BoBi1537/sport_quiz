(ns sport-quiz.frontend
  (:require [reagent.dom.client :as rd]
            [reagent.core :as r]
            [ajax.core :refer [POST]]
            [ajax.core :as ajax]))

(defonce app-root (r/atom nil))
(defonce game-state (r/atom nil))
(defonce last-answer-correct? (r/atom nil))
(defonce correct-answer (r/atom nil))
(defonce last-chosen-answer (r/atom nil))
(defonce session-id (r/atom nil))
(defonce time-remaining (r/atom nil))
(defonce timer-handle (r/atom nil))

(def max-time-equipment 10)
(def next-question-delay 1500)

(defn normalize-game-id [gid]
  (cond
    (keyword? gid) gid
    (string? gid) (keyword gid)
    :else gid))


(defn stop-timer! []
  (when @timer-handle
    (js/clearInterval @timer-handle)
    (reset! timer-handle nil))
  (when @time-remaining
    (reset! time-remaining nil)))

(declare start-question-timer!)
(defn auto-advance []
  (stop-timer!)
  (let [sid @session-id
        l-correct? last-answer-correct?
        l-correct-answer correct-answer
        l-chosen-answer last-chosen-answer
        l-game-state game-state]
    (js/console.log "AUTO-ADVANCE: Sending null answer for timeout.")
    (POST "/api/answer"
      {:params {:session-id sid :answer nil}
       :format :json
       :response-format (ajax/json-response-format {:keywords? true})
       :handler
       (fn [{:keys [:correct-answer  state] :as resp}]

         (let [new-state state]
           (js/console.log "AUTO-ADVANCE: Server response received (Timeout). New state valid?:" (some? new-state))
           (reset! l-correct? false)
           (reset! l-correct-answer correct-answer)
           (reset! l-chosen-answer "Timeout")

           (js/setTimeout
            (fn []
              (js/console.log "AUTO-ADVANCE: Timer expired. Attempting cleanup and transition.")

              (if (and new-state (:current-question new-state))
                (do
                  (reset! l-correct? nil)
                  (reset! l-chosen-answer nil)
                  (reset! l-correct-answer nil)
                  (reset! l-game-state new-state)
                  (start-question-timer! (:game-id new-state)))
                (js/console.error "AUTO-ADVANCE: ERROR! New state is invalid or missing current-question." new-state))

              (js/console.log "AUTO-ADVANCE: Transition complete.")))
           next-question-delay))
       :error-handler #(js/console.error "Timeout error:" %)})))


(defn start-question-timer! [game-id]
  (stop-timer!)
  (let [gid (normalize-game-id game-id)]
    (if (= gid :equipment)
      (let [tr time-remaining
            th timer-handle]
        (reset! tr max-time-equipment)
        (reset! th
                (js/setInterval
                 (fn []
                   (swap! tr dec)
                   (when (<= @tr 0)
                     (auto-advance)))
                 1000)))
      (reset! time-remaining nil))))

(defn start-game [game-id n]
  (stop-timer!)
  (reset! last-answer-correct? nil)
  (reset! correct-answer nil)
  (reset! last-chosen-answer nil)
  (reset! time-remaining nil)

  (POST "/api/start"
    {:params {:game game-id :n n}
     :format :json
     :response-format (ajax/json-response-format {:keywords? true})
     :handler (fn [resp]
                (js/console.log "START-GAME: Initial state loaded." (:state resp))
                (reset! session-id (:session-id resp))
                (reset! game-state (:state resp))
                (start-question-timer! (:game-id (:state resp))))
     :error-handler #(js/console.error "Start error:" %)}))

(defn submit-answer [answer]
  (when-let [sid @session-id]
    (stop-timer!)
    (reset! last-chosen-answer answer)

    (js/console.log "SUBMIT-ANSWER: Submitting answer:" answer)

    (let [l-correct? last-answer-correct?
          l-correct-answer correct-answer
          l-game-state game-state]

      (POST "/api/answer"
        {:params {:session-id sid :answer answer}
         :format :json
         :response-format (ajax/json-response-format {:keywords? true})
         :handler
         (fn [{:keys [correct? :correct-answer  state] :as resp}]

           (let [new-state state]

             (js/console.log "SUBMIT-ANSWER: Server response received. New state valid?:" (some? new-state))

             (reset! l-correct? correct?)
             (reset! l-correct-answer (if correct? answer correct-answer))

             (reset! time-remaining nil)

             (if-not (:completed? new-state)
               (js/setTimeout
                (fn []
                  (js/console.log "SUBMIT-ANSWER: Transition to next question.")

                  (if (and new-state (:current-question new-state))
                    (do
                      (reset! l-correct? nil)
                      (reset! last-chosen-answer nil)
                      (reset! l-correct-answer nil)

                      (reset! l-game-state new-state)
                      (start-question-timer! (:game-id new-state)))
                    (js/console.error "SUBMIT-ANSWER: ERROR! New state is invalid or missing current-question." new-state)))
                next-question-delay)
               (js/setTimeout
                (fn []
                  (js/console.log "SUBMIT-ANSWER: Game completed. Score:" (:score new-state) ". Delay finished.")
                  (reset! l-game-state new-state)
                  (reset! l-correct? nil)
                  (reset! last-chosen-answer nil)
                  (reset! l-correct-answer nil))
                next-question-delay))))
         :error-handler #(js/console.error "Submit error:" %)}))))


(defn progress-bar [state]
  (let [{:keys [index total]} state
        current (inc (or index 0))
        perc (if (and total (pos? total)) (Math/floor (* (/ current total) 100)) 0)]
    [:div {:class "w-full bg-gray-200 rounded-full h-4 mb-4"}
     [:div {:class "bg-green-500 h-4 rounded-full transition-all duration-500 ease-in-out flex items-center justify-end pr-2 font-medium text-xs text-white"
            :style {:width (str perc "%")}}
      (str current "/" total)]]))

(defn question-view []
  (fn []
    (when-let [state @game-state]
      (when-let [q (:current-question state)]
        (let [chosen @last-chosen-answer
              correct-display @correct-answer
              img-match (re-find #"[A-Za-z0-9_-]+\.(png|jpg|jpeg|gif)" (:prompt q))
              img (if (vector? img-match) (first img-match) img-match)
              time @time-remaining]
          [:div {:class "space-y-6"}
           [progress-bar state]
           (when (some? time)
             [:div {:class (str "text-right text-xl font-bold transition-colors duration-300 "
                                (if (<= time 3) "text-red-500 animate-pulse" "text-gray-700"))}
              (str "Time left: " time "s")])
           (when img
             [:div {:class "flex justify-center my-6"}
              [:img {:src (str "/images/" img)
                     :alt "Quiz Image"
                     :class "max-h-64 w-auto rounded-lg shadow-lg border border-gray-300 object-contain"
                     :on-error #(set! (.-src %) "https://placehold.co/300x200/cccccc/333333?text=Image+Unavailable")}]])
           [:h3 {:class "text-2xl font-semibold text-center text-gray-800 mb-6"}
            (re-find #".*:" (:prompt q))]
           (when-let [correct? @last-answer-correct?]
             [:div {:class (str "p-4 rounded-lg font-bold text-center mb-6 transition-all duration-300 shadow-md "
                                (if correct? "bg-green-500 text-white" "bg-red-500 text-white"))}
              (cond
                (= correct? true) "Correct!"
                (and (= correct? false) (= chosen "Timeout")) (str "Time Out! The correct answer was: " correct-display)
                (and (= correct? false) (some? chosen)) (str "Wrong! The correct answer was: " correct-display))])
           [:div {:class "grid grid-cols-1 gap-4"}
            (for [opt (:options q)]
              (let [is-correct (= opt correct-display)
                    is-wrong (and (false? @last-answer-correct?) (= opt chosen) (not is-correct))
                    is-selected-for-feedback (or (= opt chosen) (and (false? @last-answer-correct?) (= opt correct-display)))
                    disabled? (some? @last-answer-correct?)

                    button-class (cond-> "w-full py-4 px-6 text-lg font-medium rounded-xl transition-all duration-200 shadow-md transform hover:scale-[1.02]"
                                   (not disabled?) (str " bg-white text-gray-800 border-2 border-indigo-200 hover:bg-indigo-100 ")
                                   disabled? (str " cursor-not-allowed ")
                                   is-correct (str " !bg-green-500 !text-white !shadow-lg !shadow-green-300 ")
                                   is-wrong (str " !bg-red-500 !text-white !shadow-lg !shadow-red-300 ")
                                   (and disabled? (not is-correct) (not is-wrong)) (str " opacity-50 bg-gray-100 text-gray-500"))]
                ^{:key opt}
                [:button {:on-click #(submit-answer opt)
                          :disabled disabled?
                          :class button-class}
                 opt]))]])))))

(defn app []
  (fn []
    (let [state @game-state]
      [:div {:class "min-h-screen bg-gray-100 flex items-start justify-center p-4 sm:p-8 font-sans"}
       [:div {:class "w-full max-w-xl bg-white p-6 sm:p-10 rounded-2xl shadow-2xl"}
        [:h1 {:class "text-4xl font-extrabold text-center mb-8 text-indigo-600"} "Sport Quiz"]
        (cond
          (nil? state)
          [:div {:class "text-center space-y-6"}
           [:h2 {:class "text-2xl font-bold text-gray-700"} "Choose a game"]
           [:button {:on-click #(start-game :equipment 5)
                     :class "bg-indigo-500 hover:bg-indigo-600 text-white font-semibold py-3 px-6 rounded-xl text-xl shadow-lg transition-colors duration-200 transform hover:scale-[1.02]"}
            "Start Equipment Quiz (5 Questions)"]]
          (:completed? state)
          [:div {:class "text-center space-y-6"}
           [:h2 {:class "text-3xl font-extrabold text-green-600"} "Quiz Finished! 🎉"]
           [:p {:class "text-2xl font-semibold text-gray-800"} (str "Final Score: " (:score state))]
           [:button {:on-click #(start-game :equipment 5)
                     :class "bg-indigo-500 hover:bg-indigo-600 text-white font-semibold py-3 px-6 rounded-xl text-lg shadow-md transition-colors duration-200"}
            "Play Again"]]
          :else
          [:div {:class "space-y-4"}
           [:h2 {:class "text-xl font-bold text-indigo-500"} (str "Question " (inc (:index state)) " of " (:total state))]
           [question-view]
           [:p {:class "mt-6 text-xl font-bold text-right text-gray-800"} (str "Current Score: " (:score state))]])]])))

(defn ^:export init []
  (let [tailwind-script (js/document.createElement "script")]
    (set! (.-src tailwind-script) "https://cdn.tailwindcss.com")
    (.appendChild (.-head js/document) tailwind-script))
  (set! (.-className (.-body js/document)) "font-sans")
  (let [container (.getElementById js/document "app")]
    (when (nil? @app-root)
      (reset! app-root (rd/create-root container)))
    (.render @app-root (r/as-element [app]))))