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
      [:div {:class "animate-in fade-in slide-in-from-bottom-4 duration-500"}
       (case game-id
         :matching [matching-game-view]
         :equipment [classic-quiz-view]
         :athlete [classic-quiz-view]
         [:div "Error: Unknown Game Type"])])))

(defn app []
  (fn []
    (let [l-state @state/game-state
          dark? @state/dark-mode?
          game-id-k (normalize-game-id (:game-id l-state))
          display-total-score (+ (or @state/total-score 0) (or (:score l-state) 0))]
      [:div {:class (str "min-h-screen flex items-start justify-center p-4 sm:p-8 font-sans transition-colors duration-500 "
                         (if dark? "bg-gray-900" "bg-gray-100"))}
       [:div {:class (str "w-full max-w-xl p-6 sm:p-10 rounded-3xl shadow-2xl transition-all duration-500 "
                          (if dark? "bg-gray-800 text-white" "bg-white text-gray-800"))}
        [:div {:class "flex justify-between items-center mb-8"}
         [:button {:on-click #(swap! state/dark-mode? not)
                   :class "p-2 rounded-full hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"}
          (if dark? "🌙" "☀️")]
         [:h1 {:class (str "text-4xl font-black text-center transition-colors "
                           (if dark? "text-indigo-400" "text-indigo-600"))} "Sport Quiz"]
         [:div {:class "w-8"}]]
        (cond
          (nil? l-state)
          [:div {:class "text-center space-y-6 animate-in zoom-in duration-300"}
           [:p {:class (if dark? "text-gray-400" "text-gray-600")} "Choose your challenge:"]
           [:div {:class "grid grid-cols-1 gap-4"}
            [game-selection-button :equipment "Equipment Quiz" 5]
            [game-selection-button :athlete "Athlete Quiz" 5]
            [game-selection-button :matching "Matching Mania" 1]
            [:button {:on-click #(api/start-full-game)
                      :class (str "mt-4 py-4 px-6 rounded-2xl font-bold text-xl shadow-lg transform hover:scale-105 transition-all "
                                  (if dark? "bg-indigo-600 hover:bg-indigo-500 text-white" "bg-indigo-500 hover:bg-indigo-600 text-white"))}
             "🚀 START FULL ADVENTURE"]]]
          (:completed? l-state)
          [:div {:class "text-center space-y-6 animate-in bounce-in duration-700"}
           [:h2 {:class "text-3xl font-extrabold text-green-500"} "Quiz Finished! 🎉"]
           [:p {:class "text-2xl font-semibold"} (str "Total Score: " display-total-score)]
           [:button {:on-click #(do (reset! state/game-state nil) (reset! state/total-score 0))
                     :class "w-full bg-indigo-500 text-white py-4 rounded-xl font-bold"}
            "Play Again"]]
          :else
          [:div {:class "space-y-4"}
           [:div {:class "flex justify-between items-center"}
            [:h2 {:class "text-xl font-bold text-indigo-400"}
             (str "Game: " (str/capitalize (name (or game-id-k ""))))]
            [:div {:class (str "text-sm font-bold px-3 py-1 rounded-full "
                               (if dark? "bg-indigo-900 text-indigo-200" "bg-indigo-100 text-indigo-700"))}
             (str "Level Score: " (:score l-state) " / " (:total l-state))]]
           [question-view]
           [:div {:class (str "mt-6 flex justify-between items-center border-t pt-4 "
                              (if dark? "border-gray-700" "border-gray-100"))}
            [:div {:class (if dark? "text-gray-400" "text-gray-500")} "Total Adventure Score:"]
            [:div {:class "text-2xl font-black text-indigo-500"} display-total-score]]])]])))

(defn ^:export init []
  (let [tailwind-script (js/document.createElement "script")]
    (set! (.-src tailwind-script) "https://cdn.tailwindcss.com")
    (.appendChild (.-head js/document) tailwind-script))
  (let [container (.getElementById js/document "app")]
    (when (nil? @state/app-root)
      (reset! state/app-root (rd/create-root container)))
    (.render @state/app-root (r/as-element [app]))))