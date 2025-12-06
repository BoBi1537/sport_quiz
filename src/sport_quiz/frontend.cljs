(ns sport-quiz.frontend
  (:require [reagent.dom.client :as rd]
            [reagent.core :as r]
            [ajax.core :refer [POST]]
            [ajax.core :as ajax]
            [clojure.string :as str]))

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
(def max-time-equipment 10)
(def max-time-athlete 15)
(def max-time-matching 60)
(def next-question-delay 1500)

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

(defn stop-timer! []
  (when @timer-handle
    (js/clearInterval @timer-handle)
    (reset! timer-handle nil))
  (when @time-remaining
    (reset! time-remaining nil)))

(declare auto-advance)

(defn start-question-timer! [game-id]
  (stop-timer!)
  (let [max-time (get-max-time game-id)]
    (if max-time
      (let [tr time-remaining
            th timer-handle]
        (reset! tr max-time)
        (reset! th
                (js/setInterval
                 (fn []
                   (swap! tr dec)
                   (when (<= @tr 0)
                     (auto-advance)))
                 1000)))
      (reset! time-remaining nil))))

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
       (fn [{:keys [correct-answer state] :as resp}]
         (let [new-state state
               game-id (normalize-game-id (:game-id new-state))]

           (js/console.log "AUTO-ADVANCE: Server response received (Timeout). New state valid?:" (some? new-state))
           (when-not (= game-id :matching)
             (reset! l-correct? false)
             (reset! l-correct-answer correct-answer)
             (reset! l-chosen-answer "Timeout"))
           (js/setTimeout
            (fn []
              (js/console.log "AUTO-ADVANCE: Timer expired. Attempting cleanup and transition.")
              (reset! l-game-state new-state)
              (if (:completed? new-state)
                (js/console.log "AUTO-ADVANCE: Quiz completed after timeout.")
                (if (and new-state (:current-question new-state))
                  (do
                    (reset! l-correct? nil)
                    (reset! l-chosen-answer nil)
                    (reset! l-correct-answer nil)
                    (start-question-timer! (:game-id new-state)))
                  (js/console.error "AUTO-ADVANCE: ERROR! New state is invalid or missing current-question." new-state))))
            next-question-delay))
         :error-handler #(js/console.error "Timeout error:" %))})))


(defn start-game [game-id n]
  (stop-timer!)
  (reset! last-answer-correct? nil)
  (reset! correct-answer nil)
  (reset! last-chosen-answer nil)
  (reset! time-remaining nil)
  (reset! selected-match-item nil)
  (reset! prompt-to-answer-map nil)

  (POST "/api/start"
    {:params {:game game-id :n n}
     :format :json
     :response-format (ajax/json-response-format {:keywords? true})
     :handler (fn [resp]
                (js/console.log "START-GAME: Initial state loaded." (:state resp))
                (reset! session-id (:session-id resp))
                (let [state (:state resp)
                      game-id-k (normalize-game-id (:game-id state))]
                  (when (= game-id-k :matching)
                    (let [{:keys [prompts answers]} (:current-question state)]
                      (reset! prompt-to-answer-map (zipmap prompts answers))))
                  (reset! game-state state)
                  (start-question-timer! (:game-id state))))
     :error-handler #(js/console.error "Start error:" %)}))

(defn submit-answer [answer]
  (when-let [sid @session-id]

    (if (= (:game-id @game-state) :matching)
      (let [item1 @selected-match-item
            item2 answer
            chosen-pair (sort [item1 item2])]

        (cond
          (let [solved-pairs (get-in @game-state [:game-specific-data :solved-pairs] [])
                wrong-prompts (get-in @game-state [:game-specific-data :wrongly-paired-prompts] [])]
            (or
             (some #(or (= (first %) item2) (= (second %) item2)) solved-pairs)
             (some #(= % item2) wrong-prompts)))
          (js/console.log "MATCH: Item already solved or wrongly paired, ignoring.")
          (nil? item1)
          (reset! selected-match-item item2)
          (= item1 item2)
          (reset! selected-match-item nil)
          :else
          (do
            (reset! last-chosen-answer chosen-pair)
            (reset! selected-match-item nil)
            (js/console.log "SUBMIT-MATCH: Submitting pair:" chosen-pair)
            (let [l-correct? last-answer-correct?
                  l-game-state game-state]
              (POST "/api/answer"
                {:params {:session-id sid :answer chosen-pair}
                 :format :json
                 :response-format (ajax/json-response-format {:keywords? true})
                 :handler
                 (fn [{:keys [correct? state] :as resp}]
                   (let [new-state state]
                     (reset! l-correct? correct?)

                     (if correct?
                       (do
                         (reset! l-game-state new-state)
                         (reset! last-chosen-answer nil)
                         (reset! l-correct? nil))
                       (js/setTimeout
                        (fn []
                          (reset! l-game-state new-state)
                          (reset! l-correct? nil)
                          (reset! last-chosen-answer nil))
                        next-question-delay))
                     (when (:completed? new-state)
                       (stop-timer!)
                       (js/setTimeout
                        #(reset! l-game-state new-state)
                        next-question-delay))))

                 :error-handler #(js/console.error "Match error:" %)})))))
      (do
        (stop-timer!)
        (reset! last-chosen-answer answer)
        (let [l-correct? last-answer-correct?
              l-correct-answer correct-answer
              l-game-state game-state]
          (POST "/api/answer"
            {:params {:session-id sid :answer answer}
             :format :json
             :response-format (ajax/json-response-format {:keywords? true})
             :handler
             (fn [{:keys [correct? correct-answer state] :as resp}]
               (let [new-state state]
                 (reset! l-correct? correct?)
                 (reset! l-correct-answer (if correct? answer correct-answer))
                 (reset! time-remaining nil)
                 (if-not (:completed? new-state)
                   (js/setTimeout
                    (fn []
                      (reset! l-game-state new-state)
                      (reset! l-correct? nil)
                      (reset! last-chosen-answer nil)
                      (reset! l-correct-answer nil)
                      (start-question-timer! (:game-id new-state)))
                    next-question-delay)
                   (js/setTimeout
                    #(reset! l-game-state new-state)
                    next-question-delay))))
             :error-handler #(js/console.error "Submit error:" %)}))))))


(defn progress-bar [state]
  (let [{:keys [index total]} state
        current (inc (or index 0))
        perc (if (and total (pos? total)) (Math/floor (* (/ current total) 100)) 0)]
    [:div {:class "w-full bg-gray-200 rounded-full h-4 mb-4"}
     [:div {:class "bg-green-500 h-4 rounded-full transition-all duration-500 ease-in-out flex items-center justify-end pr-2 font-medium text-xs text-white"
            :style {:width (str perc "%")}}
      (str current "/" total)]]))

(defn time-remaining-view [time]
  (when time
    [:div {:class "text-center text-red-600 text-3xl font-bold mb-4 bg-red-100 p-2 rounded-lg shadow"}
     (str "⏰ " time "s")]))


(defn handle-match-click [item]
  (let [current-selection @selected-match-item
        state @game-state
        solved-pairs (get-in state [:game-specific-data :solved-pairs] [])
        wrong-prompts (get-in state [:game-specific-data :wrongly-paired-prompts] [])]

    (cond
      (or
       (some #(or (= (first %) item) (= (second %) item)) solved-pairs)
       (some #(= % item) wrong-prompts))
      (js/console.log "MATCH: Item already solved or wrongly paired.")
      (nil? current-selection)
      (reset! selected-match-item item)
      (= current-selection item)
      (reset! selected-match-item nil)
      :else
      (submit-answer item))))


(defn matching-game-view []
  (fn []
    (let [state @game-state
          q (:current-question state)
          prompts (:prompts q)
          answers (:answers q)
          solved-pairs (get-in state [:game-specific-data :solved-pairs] [])
          wrongly-paired-prompts (get-in state [:game-specific-data :wrongly-paired-prompts] []) ;; Novo stanje
          total-pairs (:total state)
          selected @selected-match-item
          feedback-pair @last-chosen-answer
          feedback-correct? @last-answer-correct?
          completed? (:completed? state)
          p-to-a-map @prompt-to-answer-map]
      (when (seq prompts)
        [:div {:class "space-y-6"}
         [:h3 {:class "text-2xl font-semibold text-center text-gray-800 mb-6"}
          (:prompt q)]
         [:div {:class "text-center text-xl font-bold text-indigo-600"}
          (str "Solved: " (count solved-pairs) " / " total-pairs)]
         [time-remaining-view @time-remaining]
         [:div {:class "grid grid-cols-2 gap-4 mt-6"}
          [:div {:class "space-y-4"}
           (for [item prompts]
             (let [is-selected (= selected item)
                   is-solved (some #(= (first %) item) solved-pairs)
                   is-wrongly-paired (some #(= % item) wrongly-paired-prompts)
                   is-feedback-wrong (and (false? feedback-correct?)
                                          (some #(= item %) feedback-pair))
                   is-unsolved (and completed? (not is-solved) (not is-wrongly-paired))
                   is-disabled (or is-solved is-wrongly-paired (some? feedback-pair))]
               (let [button-class (cond-> "w-full py-4 px-6 text-lg font-medium rounded-xl transition-all duration-200 shadow-md text-left"
                                    is-solved (str " !bg-green-500 !text-white opacity-100 cursor-default")
                                    is-wrongly-paired (str " !bg-red-500 !text-white opacity-100 cursor-default")
                                    is-unsolved (str " !bg-red-500 !text-white opacity-75 cursor-default")
                                    is-selected (str " bg-yellow-400 text-gray-900 border-2 border-yellow-600 shadow-lg transform scale-[1.04]")
                                    is-feedback-wrong (str " !bg-red-500 !text-white animate-pulse")
                                    (and (not is-solved) (not is-selected) (not is-feedback-wrong) (not is-wrongly-paired) (not completed?))
                                    (str " bg-white text-gray-800 border-2 border-indigo-200 hover:bg-indigo-100")
                                    is-disabled (str " cursor-not-allowed"))]
                 ^{:key item}
                 [:button {:on-click #(handle-match-click item)
                           :disabled (or is-disabled completed?)
                           :class button-class}
                  (if completed?
                    (str item " ➡️ " (get p-to-a-map item "N/A"))
                    item)])))]
          [:div {:class "space-y-4"}
           (for [item answers]
             (let [is-selected (= selected item)
                   is-solved (some #(= (second %) item) solved-pairs)
                   is-feedback-wrong (and (false? feedback-correct?)
                                          (some #(= item %) feedback-pair))
                   is-disabled (or is-solved (some? feedback-pair))]
               (let [button-class (cond-> "w-full py-4 px-6 text-lg font-medium rounded-xl transition-all duration-200 shadow-md text-left"
                                    is-solved (str " !bg-green-500 !text-white opacity-100 cursor-default")
                                    is-selected (str " bg-yellow-400 text-gray-900 border-2 border-yellow-600 shadow-lg transform scale-[1.04]")
                                    is-feedback-wrong (str " !bg-red-500 !text-white animate-pulse")
                                    (and (not is-solved) (not is-selected) (not is-feedback-wrong) (not completed?))
                                    (str " bg-white text-gray-800 border-2 border-indigo-200 hover:bg-indigo-100")
                                    is-disabled (str " cursor-not-allowed"))]
                 ^{:key item}
                 [:button {:on-click #(handle-match-click item)
                           :disabled (or is-disabled completed?)
                           :class button-class}
                  item])))]]]))))


(defn classic-quiz-view []
  (fn []
    (let [state @game-state
          q (:current-question state)]
      (when q
        (let [chosen @last-chosen-answer
              correct-display @correct-answer
              prompt-text (:prompt q)
              img-match (re-find #"[A-Za-z0-9_-]+\.(png|jpg|jpeg|gif)" prompt-text)
              img (if (vector? img-match) (first img-match) img-match)
              time @time-remaining]
          [:div {:class "space-y-6"}
           [progress-bar state]
           [time-remaining-view time]
           (when img
             [:div {:class "text-center my-6"}
              [:img {:src (str "/images/" img)
                     :class "max-h-60 max-w-full mx-auto border-2 border-gray-300 rounded-lg shadow-md"
                     :alt (str "Image for question: " prompt-text)}]])
           [:h3 {:class "text-xl font-semibold text-center text-gray-800"}
            (str/replace prompt-text #"[A-Za-z0-9_-]+\.(png|jpg|jpeg|gif)\?" "")]
           (when-let [correct? @last-answer-correct?]
             [:div {:class (str "p-3 text-lg font-bold text-white rounded-lg mb-4 shadow-md "
                                (if correct? "bg-green-500" "bg-red-500"))}
              (if correct? "Correct! Well done!" (str "Wrong! Correct answer was: " correct-display))])
           [:div {:class "grid grid-cols-1 gap-4"}
            (for [opt (:options q)]
              (let [is-correct (= opt correct-display)
                    is-wrong (and (false? @last-answer-correct?) (= opt chosen) (not is-correct))
                    disabled? (some? @last-answer-correct?)
                    button-class (cond-> "w-full py-4 px-6 text-lg font-medium rounded-xl transition-all duration-200 shadow-md transform hover:scale-[1.02]"
                                   (not disabled?) (str " bg-white text-gray-800 border-2 border-indigo-200 hover:bg-indigo-100 ")
                                   disabled? (str " cursor-not-allowed ")
                                   (and disabled? is-correct) (str " !bg-green-500 !text-white !shadow-lg !shadow-green-300 ")
                                   (and disabled? is-wrong) (str " !bg-red-500 !text-white !shadow-lg !shadow-red-300 ")
                                   (and disabled? (not is-correct) (not is-wrong)) (str " opacity-50 bg-gray-100 text-gray-500"))]
                ^{:key opt}
                [:button {:on-click #(submit-answer opt)
                          :disabled disabled?
                          :class button-class}
                 opt]))]])))))

(defn question-view []
  (fn []
    (let [state @game-state
          game-id (keyword (:game-id state))]
      (case game-id
        :matching [matching-game-view]
        :equipment [classic-quiz-view]
        :athlete [classic-quiz-view]
        [:div "Error: Unknown Game Type"]))))

(defn game-selection-button [game-id title n]
  [:button {:on-click #(start-game (name game-id) n)
            :class "bg-indigo-500 hover:bg-indigo-600 text-white font-semibold py-3 px-6 rounded-xl text-xl shadow-lg transition-colors duration-200 transform hover:scale-[1.02] w-full"}
   title])

(defn app []
  (fn []
    (let [state @game-state]
      [:div {:class "min-h-screen bg-gray-100 flex items-start justify-center p-4 sm:p-8 font-sans"}
       [:div {:class "w-full max-w-xl bg-white p-6 sm:p-10 rounded-2xl shadow-2xl"}
        [:h1 {:class "text-4xl font-extrabold text-center mb-8 text-indigo-600"} "Sport Quiz"]
        (cond
          (nil? state)
          [:div {:class "text-center space-y-6"}
           [:h2 {:class "text-2xl font-bold text-gray-700"} "Choose Game Mode: Singleplayer"]
           [game-selection-button :equipment "1. Equipment Quiz (5 Q)" 5]
           [game-selection-button :athlete "2. Athlete Quiz (10 Q)" 10]
           [game-selection-button :matching "3. Matching Game (6 P)" 6]
           [:div {:class "mt-8"}
            [:button {:class "bg-gray-400 hover:bg-gray-500 text-white font-semibold py-3 px-6 rounded-xl text-xl w-full cursor-not-allowed opacity-75"}
             "4. Start Multiplayer (Coming Soon)"]]]
          (:completed? state)
          [:div {:class "text-center space-y-6"}
           [:h2 {:class "text-3xl font-extrabold text-green-600"} "Quiz Finished! 🎉"]
           [:p {:class "text-2xl font-semibold text-gray-800"} (str "Final Score: " (:score state))]
           [:button {:on-click #(reset! game-state nil)
                     :class "bg-indigo-500 hover:bg-indigo-600 text-white font-semibold py-3 px-6 rounded-xl text-lg shadow-md transition-colors duration-200"}
            "Go to Selection"]]
          :else
          [:div {:class "space-y-4"}
           [:h2 {:class "text-xl font-bold text-indigo-500"}
            (str "Game: " (str/capitalize (:game-id state))
                 " - Question " (inc (or (:index state) 0))
                 " of " (:total state))]
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