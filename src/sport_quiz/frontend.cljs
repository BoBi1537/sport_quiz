(ns sport-quiz.frontend
  (:require [reagent.dom.client :as rd]
            [reagent.core :as r]
            [ajax.core :refer [POST]]
            [ajax.core :as ajax]
            [cljs.reader :as reader]))

(defonce app-root (r/atom nil))
(defonce game-state (r/atom nil))
(defonce last-answer-correct? (r/atom nil))
(defonce correct-answer (r/atom nil))
(defonce last-chosen-answer (r/atom nil))
(defonce session-id (r/atom nil))
(defn start-game [game-id n]
  (reset! last-answer-correct? nil)
  (reset! correct-answer nil)
  (reset! last-chosen-answer nil)

  (POST "/api/start"
    {:params {:game game-id :n n}
     :format :json
     :response-format (ajax/json-response-format {:keywords? true})
     :handler (fn [resp]
                (reset! session-id (:session-id resp))
                (reset! game-state (:state resp)))
     :error-handler #(js/console.error "Start error:" %)}))

(defn submit-answer [answer]
  (when-let [sid @session-id]
    (reset! last-chosen-answer answer)
    (POST "/api/answer"
      {:params {:session-id sid :answer answer}
       :format :json
       :response-format (ajax/json-response-format {:keywords? true})
       :handler
       (fn [{:keys [correct? correct-answer new-state]}]
         (reset! last-answer-correct? correct?)
         (reset! correct-answer correct-answer)
         (if-not (:completed? new-state)
           (js/setTimeout
            (fn []
              (reset! last-answer-correct? nil)
              (reset! last-chosen-answer nil)
              (reset! correct-answer nil)
              (reset! game-state new-state))
            1500)
           (reset! game-state new-state)))
       :error-handler #(js/console.error "Submit error:" %)})))


(defn progress-bar [progress]
  (let [perc (Math/floor (* progress 100))]
    [:div {:style {:width "100%" :background "#ddd"}}
     [:div {:style {:width (str perc "%")
                    :height "24px"
                    :background "#4CAF50"
                    :color "white"
                    :textAlign "center"}}
      (str perc "%")]]))

(defn question-view []
  (fn []
    (when-let [q (:current-question @game-state)]
      (let [chosen @last-chosen-answer
            correct-display @correct-answer
            img-match (re-find #"[A-Za-z0-9_-]+\.(png|jpg|jpeg|gif)" (:prompt q))
            img (if (vector? img-match) (first img-match) img-match)]
        [:div
         [progress-bar (:progress @game-state)]
         (when img
           [:div {:style {:textAlign "center" :margin "20px 0"}}
            [:img {:src (str "/images/" img)
                   :style {:maxHeight "200px"
                           :border "1px solid #ccc"}}]])

         [:h3 (:prompt q)]


         (when-let [correct? @last-answer-correct?]
           [:div {:style {:padding "10px"
                          :background (if correct? "#4CAF50" "#f44336")
                          :color "white"
                          :marginBottom "10px"}}
            (if correct? "Correct!" "Wrong!")])

         [:div
          (for [opt (:options q)]
            ^{:key opt}
            (let [is-correct (= opt correct-display)
                  is-wrong (and (false? @last-answer-correct?)
                                (= opt chosen)
                                (not is-correct))
                  style (cond-> {:padding "10px 20px"
                                 :margin "5px"
                                 :cursor "pointer"}
                          is-correct (assoc :background "#4CAF50" :color "white")
                          is-wrong   (assoc :background "#f44336" :color "white"))]
              [:button {:on-click #(submit-answer opt)
                        :disabled (some? @last-answer-correct?)
                        :style style}
               opt]))]]))))

(defn app []
  (fn []
    (let [state @game-state
          current-game-id (:game-id state)]
      [:div {:style {:maxWidth "600px"
                     :margin "0 auto"
                     :padding "20px"
                     :fontFamily "Arial"}}

       [:h1 {:style {:textAlign "center"}} "Sport Quiz"]

       (cond
         (nil? state)
         [:div {:style {:textAlign "center"}}
          [:h2 "Choose a game"]
          [:button {:on-click #(start-game "equipment" 5)
                    :style {:padding "15px 30px" :fontSize "1.2em"}}
           "Start Equipment Quiz"]]
         (:completed? state)
         [:div {:style {:textAlign "center"}}
          [:h2 {:style {:color "#4CAF50"}} "Finished!"]
          [:p {:style {:fontSize "1.3em"}}
           (str "Final Score: " (:score state))]
          [:button {:on-click #(start-game "equipment" 5)
                    :style {:padding "10px 20px"}}
           "Play Again"]]
         :else
         [:div
          [:h2 (str "Game: " (name current-game-id))]
          [question-view]
          [:p {:style {:marginTop "20px"
                       :fontSize "1.1em"}}
           (str "Score: " (:score state))]])])))

(defn ^:export init []
  (let [container (.getElementById js/document "app")]
    (when (nil? @app-root)
      (reset! app-root (rd/create-root container)))
    (.render @app-root (r/as-element [app]))))