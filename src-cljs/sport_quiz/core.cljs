(ns sport-quiz.core
  (:require [reagent.dom.client :as rd]
            [reagent.core :as r]
            [clojure.string :as str]
            [sport-quiz.state :as state :refer [normalize-game-id get-max-time]]
            [sport-quiz.api :as api]
            [sport-quiz.components.classic-quiz :refer [classic-quiz-view]]
            [sport-quiz.components.matching-game :refer [matching-game-view]]
            [sport-quiz.components.common :refer [game-selection-button]]))

(defn question-view []
  (fn []
    (let [l-state @state/game-state
          game-id (keyword (:game-id l-state))]
      (case game-id
        :matching [matching-game-view]
        :equipment [classic-quiz-view]
        :athlete [classic-quiz-view]
        [:div "Error: Unknown Game Type"]))))

(defn app []
  (fn []
    (let [l-state @state/game-state
          game-id-k (normalize-game-id (:game-id l-state))
          display-total-score (+ (or @state/total-score 0) (or (:score l-state) 0))]
      [:div {:class "min-h-screen bg-gray-100 flex items-start justify-center p-4 sm:p-8 font-sans"}
       [:div {:class "w-full max-w-xl bg-white p-6 sm:p-10 rounded-2xl shadow-2xl"}
        [:h1 {:class "text-4xl font-extrabold text-center mb-8 text-indigo-600"} "Sport Quiz"]
        (cond
          (nil? l-state)
          [:div {:class "text-center space-y-6"}
           [:button {:on-click #(api/start-full-game) :class "..."} "🚀 START FULL ADVENTURE"]]
          (:completed? l-state)
          [:div {:class "text-center space-y-6"}
           [:h2 {:class "text-3xl font-extrabold text-green-600"} "Quiz Finished! 🎉"]
           [:p {:class "text-2xl font-semibold text-gray-800"} (str "Total Adventure Score: " display-total-score)]
           [:button {:on-click #(do (reset! state/game-state nil) (reset! state/total-score 0))
                     :class "..."} "Go to Selection"]]
          :else
          [:div {:class "space-y-4"}
           [:div {:class "flex justify-between items-center"}
            [:h2 {:class "text-xl font-bold text-indigo-500"}
             (str "Game: " (str/capitalize (name (or game-id-k ""))))]
            [:div {:class "text-sm font-bold bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full"}
             (str "Level Score: " (:score l-state) " / " (:total l-state))]]
           [question-view]
           [:div {:class "mt-6 flex justify-between items-center border-t pt-4"}
            [:div {:class "text-gray-500 font-medium"} "Total Adventure Score:"]
            [:div {:class "text-2xl font-black text-indigo-600"} display-total-score]]])]])))

(defn ^:export init []
  (let [tailwind-script (js/document.createElement "script")]
    (set! (.-src tailwind-script) "https://cdn.tailwindcss.com")
    (.appendChild (.-head js/document) tailwind-script))
  (set! (.-className (.-body js/document)) "font-sans")
  (let [container (.getElementById js/document "app")]
    (when (nil? @state/app-root)
      (reset! state/app-root (rd/create-root container)))
    (.render @state/app-root (r/as-element [app]))))