(ns sport-quiz.components.common
  (:require [sport-quiz.state :as state]
            [sport-quiz.api :as api]))

(defn progress-bar [state]
  (let [{:keys [index total game-id score]} state
        game-id-k (state/normalize-game-id game-id)
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

(defn game-selection-button [game-id title n]
  [:button {:on-click #(api/start-game (name game-id) n)
            :class "bg-indigo-500 hover:bg-indigo-600 text-white font-semibold py-3 px-6 rounded-xl text-xl shadow-lg transition-colors duration-200 transform hover:scale-[1.02] w-full"}
   title])