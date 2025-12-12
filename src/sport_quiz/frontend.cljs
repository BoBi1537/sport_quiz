(ns sport-quiz.frontend
  (:require [reagent.dom.client :as rd]
            [reagent.core :as r]
            [ajax.core :refer [POST]]
            [ajax.core :as ajax]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.core :refer [shuffle]]))

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

(defn stop-timer! []
  (when @timer-handle
    (js/clearInterval @timer-handle)
    (reset! timer-handle nil)))

(declare auto-advance)

(defn start-question-timer! [game-id]
  (stop-timer!)
  (let [max-time (get-max-time game-id)
        current-game-id (normalize-game-id game-id)]
    (if max-time
      (let [tr time-remaining
            th timer-handle]
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
                         (reset! matching-game-ended-locally true)
                         (auto-advance))
                       (auto-advance))))
                 1000)))
      (reset! time-remaining nil))))

(defn reset-matching-atoms! []
  (reset! selected-match-item nil)
  (reset! prompt-to-answer-map nil)
  (reset! shuffled-prompts-state nil)
  (reset! shuffled-answers-state nil)
  (reset! original-pairs-map nil)
  (reset! solved-pairs-local #{})
  (reset! wrongly-paired-prompts-local #{})
  (reset! matching-game-ended-locally false))

(defn auto-advance []
  (stop-timer!)
  (let [sid @session-id
        l-correct? last-answer-correct?
        l-correct-answer correct-answer
        l-chosen-answer last-chosen-answer
        l-game-state game-state
        current-game-id (normalize-game-id (:game-id @l-game-state))]

    (js/console.log "AUTO-ADVANCE: Sending null answer for timeout.")
    (let [answer (if (= current-game-id :matching) [] nil)
          delay-time (if (= current-game-id :matching) matching-game-end-delay classic-quiz-next-question-delay)]
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
               (reset! l-correct? false)
               (reset! l-correct-answer correct-answer)
               (reset! l-chosen-answer "Timeout"))
             (if (and (= current-game-id :matching) (:completed? new-state))
               (do
                 (js/console.log "MATCHING GAME END (Timeout): Showing sorted pairs for" delay-time "ms.")
                 (js/setTimeout
                  (fn []
                    (js/console.log "MATCHING GAME END: Transitioning to Final Score view.")
                    (reset! matching-game-ended-locally false)
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
                        (reset! l-correct? nil)
                        (reset! l-chosen-answer nil)
                        (reset! l-correct-answer nil)
                        (when-not (= new-game-id :matching)
                          (reset-matching-atoms!))
                        (when (= new-game-id :matching)
                          (let [{:keys [prompts answers]} (:current-question new-state)]
                            (reset! prompt-to-answer-map (zipmap prompts answers))
                            (reset! shuffled-prompts-state (shuffle prompts))
                            (reset! shuffled-answers-state (shuffle answers))))
                        (start-question-timer! (:game-id new-state)))
                      (js/console.error "AUTO-ADVANCE: ERROR! New state is invalid or missing current-question." new-state))))
                delay-time))))
         :error-handler #(js/console.error "Timeout error:" %)}))))


(defn start-game [game-id n]
  (stop-timer!)
  (reset! last-answer-correct? nil)
  (reset! correct-answer nil)
  (reset! last-chosen-answer nil)
  (reset! time-remaining nil)
  (reset-matching-atoms!)

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
                      (reset! original-pairs-map (zipmap prompts answers))
                      (reset! prompt-to-answer-map (zipmap prompts answers))
                      (reset! shuffled-prompts-state (shuffle prompts))
                      (reset! shuffled-answers-state (shuffle answers))))
                  (reset! game-state state)
                  (start-question-timer! (:game-id state))))
     :error-handler #(js/console.error "Start error:" %)}))

(defn submit-answer [answer]
  (when-let [sid @session-id]
    (let [is-matching-pair (and (vector? answer) (= 2 (count answer)))
          l-correct? last-answer-correct?
          l-chosen-answer last-chosen-answer
          l-game-state game-state
          game-id (:game-id @l-game-state)]
      (when-not is-matching-pair
        (stop-timer!))
      (if is-matching-pair
        (do
          (reset! last-chosen-answer answer)
          (js/console.log "SUBMIT-MATCH: Submitting pair:" answer)
          (POST "/api/answer"
            {:params {:session-id sid :answer answer}
             :format :json
             :response-format (ajax/json-response-format {:keywords? true})
             :handler
             (fn [{:keys [correct? state] :as resp}]
               (let [new-state state]
                 (reset! l-correct? correct?)
                 (reset! selected-match-item nil)
                 (reset! last-chosen-answer nil)
                 (if correct?
                   (do
                     (js/console.log "MATCH: Correct pair found. Storing locally.")
                     (swap! solved-pairs-local conj answer)
                     (if (:completed? new-state)
                       (do
                         (stop-timer!)
                         (reset! l-game-state new-state)
                         (reset! matching-game-ended-locally true)
                         (js/console.log "MATCHING GAME COMPLETED: Showing sorted pairs for" matching-game-end-delay "ms.")
                         (js/setTimeout
                          (fn []
                            (js/console.log "MATCHING GAME END: Transitioning to Final Score view.")
                            (reset! matching-game-ended-locally false)
                            (reset! l-game-state new-state))
                          matching-game-end-delay))
                       (reset! l-game-state new-state)))
                   (do
                     (js/console.log "MATCH: Incorrect pair selected. Storing Prompt locally for red state.")
                     (swap! wrongly-paired-prompts-local conj (first answer))
                     (reset! l-game-state new-state)))))

             :error-handler #(js/console.error "Match error:" %)}))
        (do
          (reset! last-chosen-answer answer)
          (let [l-correct-answer correct-answer]
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
                        (reset! l-chosen-answer nil)
                        (reset! l-correct-answer nil)
                        (start-question-timer! game-id))
                      classic-quiz-next-question-delay)
                     (js/setTimeout
                      #(reset! l-game-state new-state)
                      classic-quiz-next-question-delay))))
               :error-handler #(js/console.error "Submit error:" %)})))))))

(defn progress-bar [state]
  (let [{:keys [index total game-id score]} state
        game-id-k (normalize-game-id game-id)
        current-val (if (= game-id-k :matching) (or score 0) (inc (or index 0)))
        total-val (or total 1)
        perc (if (and total-val (pos? total-val)) (Math/floor (* (/ current-val total-val) 100)) 0)]
    [:div {:class "w-full bg-gray-200 rounded-full h-4 mb-4"}
     [:div {:class "bg-green-500 h-4 rounded-full transition-all duration-500 ease-in-out flex items-center justify-end pr-2 font-medium text-xs text-white"
            :style {:width (str perc "%")}}
      (str current-val "/" total-val)]]))

(defn time-remaining-view [time]
  (when (some? time)
    [:div {:class "text-center text-red-600 text-3xl font-bold mb-4 bg-red-100 p-2 rounded-lg shadow"}
     (str "⏰ " time "s")]))

(defn handle-match-click [item]
  (let [current-selection @selected-match-item
        state @game-state
        q (:current-question state)
        prompts (:prompts q)
        answers (:answers q)
        solved @solved-pairs-local
        wrong-prompts @wrongly-paired-prompts-local
        is-prompt? (fn [i] (some #(= i %) prompts))
        is-answer? (fn [i] (some #(= i %) answers))
        is-permanently-solved (or (some #(= (first %) item) solved) (some #(= (second %) item) solved))
        is-permanently-wrong-prompt (and (is-prompt? item) (contains? wrong-prompts item))
        is-disabled-permanently (or is-permanently-solved is-permanently-wrong-prompt)]

    (if is-disabled-permanently
      (do
        (js/console.log "MATCH: Ignoring click due to permanent disablement.")
        (reset! selected-match-item nil)
        nil)

      (cond
        (nil? current-selection)
        (reset! selected-match-item item)
        (= current-selection item)
        (reset! selected-match-item nil)
        (or (and (is-prompt? current-selection) (is-prompt? item))
            (and (is-answer? current-selection) (is-answer? item)))
        (do
          (reset! selected-match-item item)
          (js/console.log "MATCH: Both items from same column. New selection:" item))
        :else
        (let [item1 current-selection
              item2 item
              prompt-item (if (is-prompt? item1) item1 item2)
              answer-item (if (is-answer? item1) item1 item2)
              chosen-pair [prompt-item answer-item]]
          (submit-answer chosen-pair))))))

(defn- get-matching-button-class [item is-selected is-solved is-permanently-wrong completed-local? is-prompt?]
  (cond-> "w-full py-4 px-6 text-lg font-medium rounded-xl transition-colors duration-700 shadow-md text-left"
    is-solved (str " !bg-green-500 !text-white opacity-100 cursor-default")
    (and is-prompt? is-permanently-wrong (not completed-local?)) (str " !bg-red-500 !text-white opacity-100 cursor-default")
    (and completed-local? (not is-solved)) (str " !bg-gray-200 !text-gray-800 cursor-not-allowed")
    (and is-selected (not is-solved) (not is-permanently-wrong) (not completed-local?))
    (str " bg-yellow-400 text-gray-900 border-2 border-yellow-600 shadow-lg transform scale-[1.04]")
    (and (not is-solved) (not is-selected) (not is-permanently-wrong) (not completed-local?))
    (str " bg-white text-gray-800 border-2 border-indigo-200 hover:bg-indigo-100")
    (or is-solved is-permanently-wrong completed-local?) (str " cursor-not-allowed")))

(defn matching-game-view []
  (fn []
    (let [state @game-state
          q (:current-question state)
          _ (when (and (:prompts q) (nil? @shuffled-prompts-state) (not (:completed? state)))
              (reset! shuffled-prompts-state (shuffle (:prompts q)))
              (reset! shuffled-answers-state (shuffle (:answers q)))
              (reset! prompt-to-answer-map (zipmap (:prompts q) (:answers q))))
          completed-server? (:completed? state)
          completed-local? @matching-game-ended-locally
          list-prompts (if completed-local? (keys @original-pairs-map) @shuffled-prompts-state)
          list-answers (if completed-local? (vals @original-pairs-map) @shuffled-answers-state)
          selected @selected-match-item
          solved @solved-pairs-local
          wrong-prompts @wrongly-paired-prompts-local
          total-pairs (:total state)]

      (when (or (seq list-prompts) completed-server? completed-local?)
        [:div {:class "space-y-6"}
         [:h3 {:class "text-2xl font-semibold text-center text-gray-800 mb-6"}
          (:prompt q)]
         [:div {:class "text-center text-xl font-bold text-indigo-600"}
          (str "Solved: " (:score state) " / " total-pairs)]
         [time-remaining-view @time-remaining]
         [:div {:class "grid grid-cols-2 gap-4 mt-6"}
          [:div {:class "space-y-4"}
           (for [item list-prompts]
             (let [is-selected (= selected item)
                   is-solved (some #(= (first %) item) solved)
                   is-permanently-wrong (contains? wrong-prompts item)
                   is-disabled (or is-solved is-permanently-wrong completed-local?)]
               ^{:key item}
               [:button {:on-click #(handle-match-click item)
                         :disabled is-disabled
                         :class (get-matching-button-class item is-selected is-solved is-permanently-wrong completed-local? true)}
                item]))]
          [:div {:class "space-y-4"}
           (for [item list-answers]
             (let [is-selected (= selected item)
                   is-solved (some #(= (second %) item) solved)
                   is-disabled (or is-solved completed-local?)]
               ^{:key item}
               [:button {:on-click #(handle-match-click item)
                         :disabled is-disabled
                         :class (get-matching-button-class item is-selected is-solved false completed-local? false)}
                item]))]]]))))

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
    (let [state @game-state
          game-id-k (normalize-game-id (:game-id state))]
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
          (and (= game-id-k :matching) @matching-game-ended-locally)
          [:div {:class "space-y-4"}
           [:h2 {:class "text-xl font-bold text-indigo-500"}
            (str "Game: " (str/capitalize (:game-id state))
                 " - Score: " (:score state)
                 " / " (:total state))]
           [matching-game-view]
           [:p {:class "mt-6 text-xl font-bold text-right text-gray-800"} (str "Current Time: " (or @time-remaining (get-max-time (:game-id state))) "s")]]
          (:completed? state)
          [:div {:class "text-center space-y-6"}
           [:h2 {:class "text-3xl font-extrabold text-green-600"} "Quiz Finished! 🎉"]
           [:p {:class "text-2xl font-semibold text-gray-800"} (str "Final Score: " (:score state) " / " (:total state))]
           [:button {:on-click #(reset! game-state nil)
                     :class "bg-indigo-500 hover:bg-indigo-600 text-white font-semibold py-3 px-6 rounded-xl text-lg shadow-md transition-colors duration-200"}
            "Go to Selection"]]
          :else
          [:div {:class "space-y-4"}
           [:h2 {:class "text-xl font-bold text-indigo-500"}
            (str "Game: " (str/capitalize (:game-id state))
                 " - Score: " (:score state)
                 " / " (:total state))]
           [question-view]
           [:p {:class "mt-6 text-xl font-bold text-right text-gray-800"} (str "Current Time: " (or @time-remaining (get-max-time (:game-id state))) "s")]])]])))

(defn ^:export init []
  (let [tailwind-script (js/document.createElement "script")]
    (set! (.-src tailwind-script) "https://cdn.tailwindcss.com")
    (.appendChild (.-head js/document) tailwind-script))
  (set! (.-className (.-body js/document)) "font-sans")
  (let [container (.getElementById js/document "app")]
    (when (nil? @app-root)
      (reset! app-root (rd/create-root container)))
    (.render @app-root (r/as-element [app]))))