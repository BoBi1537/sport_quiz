(ns sport-quiz.frontend
  (:require [reagent.dom.client :as rd]
            [reagent.core :as r]
            [ajax.core :refer [POST]]
            [cljs.reader :as reader]))

(defonce app-root (r/atom nil))
(defonce game-state (r/atom nil))
(defonce last-answer-correct? (r/atom nil))
(defonce correct-answer (r/atom nil))
(defonce last-chosen-answer (r/atom nil))

(defn start-game [game-id n]
  (reset! last-answer-correct? nil)
  (reset! correct-answer nil)
  (reset! last-chosen-answer nil)
  (POST "/api/start"
    {:params {:game game-id :n n}
     :format :json
     :response-format (ajax.core/json-response-format {:keywords? true})
     :handler #(let [server-resp %]
                 (reset! game-state
                         (assoc (:state server-resp)
                                :session-id (:session-id server-resp))))
     :error-handler (fn [e] (js/console.error "AJAX Error in start-game:" e))}))

(defn submit-answer [answer]
  (when-let [session-id (:session-id @game-state)]
    (reset! last-chosen-answer answer)
    (POST "/api/answer"
      {:params {:session-id session-id :answer answer}
       :format :json
       :response-format (ajax.core/json-response-format {:keywords? true})
       :handler #(let [server-resp %
                       new-state-from-server (:state server-resp)
                       correct? (:correct? server-resp)
                       correct-ans (:correct-answer server-resp)]
                   (reset! last-answer-correct? correct?)
                   (reset! correct-answer correct-ans)
                   (let [completed? (:completed? new-state-from-server)]
                     (if-not completed?
                       (js/setTimeout (fn [] 
                                        (reset! last-answer-correct? nil)
                                        (reset! correct-answer nil)
                                        (reset! last-chosen-answer nil)
                                        (reset! game-state (assoc new-state-from-server :session-id session-id)))
                                      1500)
                       (reset! game-state (assoc new-state-from-server :session-id session-id)))))
       :error-handler (fn [e] (js/console.error "AJAX Error in submit-answer:" e))})))

(defn progress-bar [progress]
  (let [perc (Math/floor (* progress 100))]
    [:div {:style {:width "100%" :backgroundColor "#ddd"}}
     [:div {:style {:width (str perc "%") :height "24px" :backgroundColor "#4CAF50" :textAlign "center" :color "white"}}
      (str perc "%")]]))

(defn question-view []
  (fn []
    (when-let [q (:current-question @game-state)]
      (let [correct-ans-display @correct-answer
            chosen-ans-display @last-chosen-answer] 
        [:div
         [progress-bar (:progress @game-state)]

        
         (when-let [image-file-vec (re-find #"[a-zA-Z0-9_-]+\.(png|jpg|jpeg|gif)" (:prompt q))]
           (let [image-file (first image-file-vec)]
             [:div {:style {:textAlign "center" :margin "20px 0"}}
              [:img {:src (str "/images/" image-file) :alt image-file :style {:maxHeight "200px" :border "1px solid #ccc"}}]]))

         [:h3 {:style {:marginBottom "20px"}} (:prompt q)]

         (when-let [correct? @last-answer-correct?]
           [:div {:style {:padding "10px" :marginBottom "10px" :borderRadius "5px"
                          :backgroundColor (if correct? "#4CAF50" "#f44336")
                          :color "white" :fontWeight "bold"}}
            (if correct? "Correct Answer!" "Incorrect Answer!")])

         [:div
          (for [opt (:options q)]
            ^{:key opt}
            (let [is-correct-display (and correct-ans-display (= opt correct-ans-display))
                 
                  is-incorrect-choice (and (false? @last-answer-correct?)
                                           (= opt chosen-ans-display)
                                           (not is-correct-display))
                  button-style (cond-> {:padding "10px 20px" :margin "5px" :cursor "pointer"}
                                 is-correct-display (assoc :backgroundColor "#4CAF50" :color "white" :fontWeight "bold")
                                 is-incorrect-choice (assoc :backgroundColor "#f44336" :color "white" :fontWeight "bold"))]
              [:button {:on-click #(submit-answer opt)
                        :disabled (some? @last-answer-correct?) 
                        :style button-style} opt]))]]))))


(defn app []
  (fn []
    (let [state @game-state]
      [:div {:style {:maxWidth "600px" :margin "0 auto" :padding "20px" :fontFamily "Arial, sans-serif"}}
       [:h1 {:style {:textAlign "center" :color "#333"}} "Sport Quiz"]
       (cond
         (nil? state)
         [:div {:style {:textAlign "center"}}
          [:h2 "Welcome! Choose a game mode:"]
          [:p "Single Player - Equipment Quiz (5 questions)"]
          [:button {:on-click #(start-game "equipment" 5)
                    :style {:padding "15px 30px" :fontSize "1.2em" :cursor "pointer"}} "Start Single Player"]]

         (:completed? state)
         [:div {:style {:textAlign "center" :marginTop "40px"}}
          [:h2 {:style {:color "#4CAF50"}} "Game Finished!"]
          [:p {:style {:fontSize "1.5em" :fontWeight "bold"}} (str "Final Score: " (:score state) " points")]
          [:button {:on-click #(start-game "equipment" 5)
                    :style {:padding "10px 20px" :marginTop "20px" :cursor "pointer"}} "Play Equipment Quiz Again"]]

         :else
         [:div
          [question-view]
          [:p {:style {:marginTop "20px" :fontSize "1.1em"}} (str "Current Score: " (:score state))]])])))

(defn ^:export init []
  (let [container (.getElementById js/document "app")
        app-element (r/as-element [app])]
    (when (nil? @app-root)
      (reset! app-root (rd/create-root container)))
    (.render @app-root app-element)))