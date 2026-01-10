(ns sport-quiz.components.matching-game
  (:require [sport-quiz.state :as state :refer [normalize-game-id]]
            [sport-quiz.api :as api]
            [sport-quiz.components.common :refer [time-remaining-view]]))

(defn handle-match-click [item]
  (let [completed-local? @state/matching-game-ended-locally
        current-selection @state/selected-match-item
        l-state @state/game-state
        q (:current-question l-state)
        prompts (:prompts q)
        answers (:answers q)
        is-prompt? (fn [i] (some #(= i %) prompts))
        is-answer? (fn [i] (some #(= i %) answers))]

    (when-not completed-local?
      (cond
        (nil? current-selection)
        (reset! state/selected-match-item item)
        (= current-selection item)
        (reset! state/selected-match-item nil)
        (or (and (is-prompt? current-selection) (is-prompt? item))
            (and (is-answer? current-selection) (is-answer? item)))
        (reset! state/selected-match-item item)
        :else
        (let [prompt-item (if (is-prompt? current-selection) current-selection item)
              answer-item (if (is-answer? current-selection) current-selection item)]
          (api/submit-answer [prompt-item answer-item]))))))


(defn- get-matching-button-class [item is-selected is-solved is-permanently-wrong completed-local? is-prompt? dark?]
  (cond-> "w-full py-4 px-4 text-sm sm:text-base font-bold rounded-2xl transition-all duration-300 shadow-sm border-2 "
    is-solved
    (str " !bg-green-500 !border-green-500 !text-white opacity-100 cursor-default scale-95 ")
    (and is-prompt? is-permanently-wrong)
    (str " !bg-red-500 !border-red-500 !text-white opacity-100 ")
    (and completed-local? (not is-solved))
    (str " opacity-30 " (if dark? "bg-gray-800 border-gray-700" "bg-gray-100 border-gray-200"))
    (and is-selected (not completed-local?))
    (str " bg-indigo-500 text-white border-indigo-600 scale-105 z-10 ")
    (and (not is-selected) (not is-solved) (not is-permanently-wrong) (not completed-local?))
    (str (if dark? " bg-gray-700 border-gray-600 text-white hover:bg-gray-600 "
             " bg-white border-gray-100 text-gray-800 hover:border-indigo-300 hover:bg-indigo-50 "))))

(defn matching-game-view []
  (fn []
    (let [l-state @state/game-state
          dark? @state/dark-mode?
          q (:current-question l-state)
          completed-local? @state/matching-game-ended-locally
          list-prompts (if completed-local? (keys @state/original-pairs-map) @state/shuffled-prompts-state)
          list-answers (if completed-local? (vals @state/original-pairs-map) @state/shuffled-answers-state)
          selected @state/selected-match-item
          solved @state/solved-pairs-local
          wrong-prompts @state/wrongly-paired-prompts-local]
      [:div {:class "space-y-4 animate-in fade-in duration-500"}
       [:h3 {:class "text-xl font-bold text-center mb-2"}
        (if completed-local? "Time's up! Here are the pairs:" (:prompt q))]
       [time-remaining-view @state/time-remaining (:game-id l-state)]
       [:div {:class "grid grid-cols-2 gap-3 mt-4"}
        [:div {:class "space-y-3"}
         (for [item list-prompts]
           ^{:key (str "p-" item)}
           [:button {:on-click #(handle-match-click item)
                     :class (get-matching-button-class item (= selected item) (some #(= (first %) item) solved) (contains? wrong-prompts item) completed-local? true dark?)}
            item])]
        [:div {:class "space-y-3"}
         (for [item list-answers]
           ^{:key (str "a-" item)}
           [:button {:on-click #(handle-match-click item)
                     :class (get-matching-button-class item (= selected item) (some #(= (second %) item) solved) false completed-local? false dark?)}
            item])]]])))