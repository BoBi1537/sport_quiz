(ns sport-quiz.components.matching-game
  (:require [sport-quiz.state :as state :refer [normalize-game-id]]
            [sport-quiz.api :as api]
            [sport-quiz.components.common :refer [time-remaining-view]]))

(defn handle-match-click [item]
  (let [current-selection @state/selected-match-item
        l-state @state/game-state
        q (:current-question l-state)
        prompts (:prompts q)
        answers (:answers q)
        solved @state/solved-pairs-local
        wrong-prompts @state/wrongly-paired-prompts-local
        is-prompt? (fn [i] (some #(= i %) prompts))
        is-answer? (fn [i] (some #(= i %) answers))
        is-permanently-solved (or (some #(= (first %) item) solved) (some #(= (second %) item) solved))
        is-permanently-wrong-prompt (and (is-prompt? item) (contains? wrong-prompts item))
        is-disabled-permanently (or is-permanently-solved is-permanently-wrong-prompt)]
    (if is-disabled-permanently
      (do
        (js/console.log "MATCH: Ignoring click due to permanent disablement.")
        (reset! state/selected-match-item nil)
        nil)
      (cond
        (nil? current-selection)
        (reset! state/selected-match-item item)
        (= current-selection item)
        (reset! state/selected-match-item nil)
        (or (and (is-prompt? current-selection) (is-prompt? item))
            (and (is-answer? current-selection) (is-answer? item)))
        (do
          (reset! state/selected-match-item item)
          (js/console.log "MATCH: Both items from same column. New selection:" item))
        :else
        (let [item1 current-selection
              item2 item
              prompt-item (if (is-prompt? item1) item1 item2)
              answer-item (if (is-answer? item1) item1 item2)
              chosen-pair [prompt-item answer-item]]
          (api/submit-answer chosen-pair))))))

(defn- get-matching-button-class [item is-selected is-solved is-permanently-wrong completed-local? is-prompt?]
  (cond-> "w-full py-4 px-6 text-lg font-medium rounded-xl transition-colors duration-700 shadow-md text-left"
    is-solved (str " !bg-green-500 !text-white opacity-100 cursor-default")
    (and is-prompt? is-permanently-wrong (not completed-local?)) (str " !bg-red-500 !text-white opacity-100 cursor-default")
    (and completed-local? (not is-solved)) (str " !bg-gray-200 !text-gray-800 cursor-not-allowed")
    (and is-selected (not is-solved) (not is-permanently-wrong) (not completed-local?))
    (str " bg-yellow-400 text-gray-900 border-2 border-yellow-600 shadow-lg transform scale-[1.04]")
    (and (not is-solved) (not is-selected) (not is-permanently-wrong) (not completed-local?))
    (str " bg-white text-gray-800 border-2 border-indigo-200 hover:bg-indigo-100")
    (or is-solved is-permanently-wrong completed-local?) (str " cursor-not-allowed")))

(defn matching-game-view []
  (fn []
    (let [l-state @state/game-state
          q (:current-question l-state)
          _ (when (and (:prompts q) (nil? @state/shuffled-prompts-state) (not (:completed? l-state)))
              (reset! state/shuffled-prompts-state (clojure.core/shuffle (:prompts q)))
              (reset! state/shuffled-answers-state (clojure.core/shuffle (:answers q)))
              (reset! state/prompt-to-answer-map (zipmap (:prompts q) (:answers q))))
          completed-server? (:completed? l-state)
          completed-local? @state/matching-game-ended-locally
          list-prompts (if completed-local? (keys @state/original-pairs-map) @state/shuffled-prompts-state)
          list-answers (if completed-local? (vals @state/original-pairs-map) @state/shuffled-answers-state)
          selected @state/selected-match-item
          solved @state/solved-pairs-local
          wrong-prompts @state/wrongly-paired-prompts-local
          total-pairs (:total l-state)]
      (when (or (seq list-prompts) completed-server? completed-local?)
        [:div {:class "space-y-6"}
         [:h3 {:class "text-2xl font-semibold text-center text-gray-800 mb-6"}
          (:prompt q)]
         [:div {:class "text-center text-xl font-bold text-indigo-600"}
          (str "Solved: " (:score l-state) " / " total-pairs)]
         [time-remaining-view @state/time-remaining]
         [:div {:class "grid grid-cols-2 gap-4 mt-6"}
          [:div {:class "space-y-4"}
           (for [item list-prompts]
             (let [is-selected (= selected item)
                   is-solved (some #(= (first %) item) solved)
                   is-permanently-wrong (contains? wrong-prompts item)
                   is-disabled (or is-solved is-permanently-wrong completed-local?)]
               ^{:key item}
               [:button {:on-click #(handle-match-click item)
                         :disabled is-disabled
                         :class (get-matching-button-class item is-selected is-solved is-permanently-wrong completed-local? true)}
                item]))]
          [:div {:class "space-y-4"}
           (for [item list-answers]
             (let [is-selected (= selected item)
                   is-solved (some #(= (second %) item) solved)
                   is-disabled (or is-solved completed-local?)]
               ^{:key item}
               [:button {:on-click #(handle-match-click item)
                         :disabled is-disabled
                         :class (get-matching-button-class item is-selected is-solved false completed-local? false)}
                item]))]]]))))