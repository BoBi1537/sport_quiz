(ns sport-quiz.components.common
  (:require [sport-quiz.state :as state]
            [sport-quiz.api :as api]))

(defn progress-bar [state-val]
  (let [dark? @state/dark-mode?
        {:keys [index total game-id score]} state-val
        game-id-k (state/normalize-game-id game-id)
        current-val (if (= game-id-k :matching) (or score 0) (inc (or index 0)))
        total-val (or total 1)
        perc (if (and total-val (pos? total-val)) (Math/floor (* (/ current-val total-val) 100)) 0)]
    [:div {:class (str "w-full rounded-full h-3 mb-4 " (if dark? "bg-gray-700" "bg-gray-200"))}
     [:div {:class "bg-green-500 h-3 rounded-full transition-all duration-700 ease-out"
            :style {:width (str perc "%")}}]]))

(defn time-remaining-view [time game-id]
  (let [max-t (state/get-max-time game-id)
        perc (* (/ (or time 0) max-t) 100)
        color (cond (<= perc 25) "bg-red-500" (<= perc 50) "bg-yellow-500" :else "bg-indigo-500")]
    [:div {:class "w-full mb-6"}
     [:div {:class "flex justify-between text-xs font-bold mb-1 uppercase tracking-wider opacity-70"}
      [:span "Time Remaining"]
      [:span (str time "s")]]
     [:div {:class "w-full bg-gray-200 dark:bg-gray-700 rounded-full h-1.5 overflow-hidden"}
      [:div {:class (str color " h-full transition-all duration-1000 ease-linear")
             :style {:width (str perc "%")}}]]]))

(defn game-selection-button [game-id title n]
  (let [dark? @state/dark-mode?]
    [:button {:on-click #(api/start-game (name game-id) n)
              :class (str "font-semibold py-4 px-6 rounded-2xl text-lg shadow-md transition-all duration-200 transform hover:scale-[1.02] w-full "
                          (if dark? "bg-gray-700 text-white hover:bg-gray-600" "bg-white text-gray-800 border-2 border-indigo-50 hover:bg-indigo-50"))}
     title]))