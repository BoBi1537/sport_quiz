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


(defn- get-matching-button-class [item is-selected is-solved is-permanently-wrong completed-local? is-prompt?]
  (cond-> "w-full py-4 px-6 text-lg font-medium rounded-xl transition-all duration-300 shadow-md text-left"
    is-solved (str " !bg-green-500 !text-white opacity-100 cursor-default")
    (and is-prompt? is-permanently-wrong) (str " !bg-red-500 !text-white opacity-100 cursor-default")
    (and completed-local? (not is-solved)) (str " bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed")
    (and is-selected (not completed-local?)) (str " bg-yellow-400 text-gray-900 border-2 border-yellow-600 scale-[1.04]")
    (and (not is-selected) (not is-solved) (not is-permanently-wrong) (not completed-local?)) (str " bg-white text-gray-800 border-2 border-indigo-200 hover:bg-indigo-100")))

(defn matching-game-view []
  (fn []
    (let [l-state @state/game-state
          q (:current-question l-state)
          completed-local? @state/matching-game-ended-locally
          list-prompts (if completed-local? (keys @state/original-pairs-map) @state/shuffled-prompts-state)
          list-answers (if completed-local? (vals @state/original-pairs-map) @state/shuffled-answers-state)
          selected @state/selected-match-item
          solved @state/solved-pairs-local
          wrong-prompts @state/wrongly-paired-prompts-local]
      [:div {:class "space-y-6"}
       [:h3 {:class "text-2xl font-semibold text-center text-gray-800 mb-6"}
        (if completed-local? "Correct Pairs Revealed!" (:prompt q))]
       [time-remaining-view @state/time-remaining]
       [:div {:class "grid grid-cols-2 gap-4 mt-6"}
        [:div {:class "space-y-4"}
         (for [item list-prompts]
           ^{:key (str "p-" item)}
           [:button {:on-click #(handle-match-click item)
                     :class (get-matching-button-class item (= selected item) (some #(= (first %) item) solved) (contains? wrong-prompts item) completed-local? true)}
            item])]
        [:div {:class "space-y-4"}
         (for [item list-answers]
           ^{:key (str "a-" item)}
           [:button {:on-click #(handle-match-click item)
                     :class (get-matching-button-class item (= selected item) (some #(= (second %) item) solved) false completed-local? false)}
            item])]]])))