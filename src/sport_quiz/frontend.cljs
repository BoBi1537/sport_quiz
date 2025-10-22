(ns sport-quiz.frontend
  (:require [reagent.dom.client :as rd]
            [reagent.core :as r]
            [ajax.core :refer [POST]]
            [cljs.reader :as reader]))

(defonce app-root (r/atom nil))
(defonce game-state (r/atom nil))

(defn start-game [game-id]
  (POST "/api/start"
    {:params {:game game-id}
     :format :json
     :response-format (ajax.core/json-response-format {:keywords? true})
     :handler #(let [server-resp %]
                 (reset! game-state
                         (assoc (:state server-resp)
                                :session-id (:session-id server-resp))))

     :error-handler (fn [e] (js/console.error "AJAX Error in start-game:" e))}))

(defn submit-answer [answer]
  (when @game-state
    (POST "/api/answer"
      {:params {:session-id (:session-id @game-state)
                :answer answer}
       :format :json
       :response-format (ajax.core/json-response-format {:keywords? true})
       :handler #(let [new-state-from-server (:state %)
                       session-id (:session-id @game-state)]
                   (reset! game-state (assoc new-state-from-server :session-id session-id)))
       :error-handler (fn [e] (js/console.error "AJAX Error in submit-answer:" e))})))



(defn question-view []
  (fn []
    (when-let [q (:current-question @game-state)]
      [:div
       (when-let [image-file (re-find #"[a-zA-Z0-9_-]+\\.(png|jpg|jpeg|gif)" (:prompt q))]
         [:div {:style {:textAlign "center"}}
          [:img {:src (str "/" image-file) :alt image-file :style {:maxHeight "200px"}}]])
       [:p (:prompt q)]
       (for [opt (:options q)]
         ^{:key opt}
         [:button {:on-click #(submit-answer opt)} opt])])))

(defn app []
  (fn []
    (let [state @game-state]
      [:div
       [:h1 "Sport Quiz"]
       (cond
         (nil? state)
         [:div
          [:button {:on-click #(start-game "equipment")} "Start Equipment Game"]]
         (:completed? state)
         [:div
          [:h3 "Game finished"]
          [:p (str "Score: " (:score state))]
          [:button {:on-click #(start-game "equipment")} "Play again"]]
         :else
         [:div
          [question-view]
          [:p (str "Score: " (:score state))]])])))



(defn ^:export init []
  (let [container (.getElementById js/document "app")
        app-element (r/as-element [app])]
    (when (nil? @app-root)
      (reset! app-root (rd/create-root container)))
    (.render @app-root app-element)))