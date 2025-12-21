(ns sport-quiz.core
  (:require [reagent.dom.client :as rd]
            [reagent.core :as r]
            [clojure.string :as str]
            [sport-quiz.state :as state :refer [normalize-game-id get-max-time]]
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
          game-id-k (normalize-game-id (:game-id l-state))]
      [:div {:class "min-h-screen bg-gray-100 flex items-start justify-center p-4 sm:p-8 font-sans"}
       [:div {:class "w-full max-w-xl bg-white p-6 sm:p-10 rounded-2xl shadow-2xl"}
        [:h1 {:class "text-4xl font-extrabold text-center mb-8 text-indigo-600"} "Sport Quiz"]
        (cond
          (nil? l-state)
          [:div {:class "text-center space-y-6"}
           [:h2 {:class "text-2xl font-bold text-gray-700"} "Choose Game Mode: Singleplayer"]
           [game-selection-button :equipment "1. Equipment Quiz (5 Q)" 5]
           [game-selection-button :athlete "2. Athlete Quiz (10 Q)" 10]
           [game-selection-button :matching "3. Matching Game (6 P)" 6]
           [:div {:class "mt-8"}
            [:button {:class "bg-gray-400 hover:bg-gray-500 text-white font-semibold py-3 px-6 rounded-xl text-xl w-full cursor-not-allowed opacity-75"}
             "4. Start Multiplayer (Coming Soon)"]]]
          (and (= game-id-k :matching) @state/matching-game-ended-locally)
          [:div {:class "space-y-4"}
           [:h2 {:class "text-xl font-bold text-indigo-500"}
            (str "Game: " (str/capitalize (:game-id l-state))
                 " - Score: " (:score l-state)
                 " / " (:total l-state))]
           [matching-game-view]
           [:p {:class "mt-6 text-xl font-bold text-right text-gray-800"} (str "Current Time: " (or @state/time-remaining (get-max-time (:game-id l-state))) "s")]]
          (:completed? l-state)
          [:div {:class "text-center space-y-6"}
           [:h2 {:class "text-3xl font-extrabold text-green-600"} "Quiz Finished! 🎉"]
           [:p {:class "text-2xl font-semibold text-gray-800"} (str "Final Score: " (:score l-state) " / " (:total l-state))]
           [:button {:on-click #(reset! state/game-state nil)
                     :class "bg-indigo-500 hover:bg-indigo-600 text-white font-semibold py-3 px-6 rounded-xl text-lg shadow-md transition-colors duration-200"}
            "Go to Selection"]]
          :else
          [:div {:class "space-y-4"}
           [:h2 {:class "text-xl font-bold text-indigo-500"}
            (str "Game: " (str/capitalize (:game-id l-state))
                 " - Score: " (:score l-state)
                 " / " (:total l-state))]
           [question-view]
           [:p {:class "mt-6 text-xl font-bold text-right text-gray-800"} (str "Current Time: " (or @state/time-remaining (get-max-time (:game-id l-state))) "s")]])]])))

(defn ^:export init []
  (let [tailwind-script (js/document.createElement "script")]
    (set! (.-src tailwind-script) "https://cdn.tailwindcss.com")
    (.appendChild (.-head js/document) tailwind-script))
  (set! (.-className (.-body js/document)) "font-sans")
  (let [container (.getElementById js/document "app")]
    (when (nil? @state/app-root)
      (reset! state/app-root (rd/create-root container)))
    (.render @state/app-root (r/as-element [app]))))