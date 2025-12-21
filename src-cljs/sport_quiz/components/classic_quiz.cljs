(ns sport-quiz.components.classic-quiz
  (:require [clojure.string :as str]
            [sport-quiz.state :as state]
            [sport-quiz.api :as api]
            [sport-quiz.components.common :refer [progress-bar time-remaining-view]]))

(defn classic-quiz-view []
  (fn []
    (let [state @state/game-state
          q (:current-question state)]
      (when q
        (let [chosen @state/last-chosen-answer
              correct-display @state/correct-answer
              prompt-text (:prompt q)
              img-match (re-find #"[A-Za-z0-9_-]+\.(png|jpg|jpeg|gif)" prompt-text)
              img (if (vector? img-match) (first img-match) img-match)
              time @state/time-remaining]
          [:div {:class "space-y-6"}
           [progress-bar state]
           [time-remaining-view time]
           (when img
             [:div {:class "text-center my-6"}
              [:img {:src (str "/images/" img)
                     :class "max-h-60 max-w-full mx-auto border-2 border-gray-300 rounded-lg shadow-md"
                     :alt (str "Image for question: " prompt-text)}]])
           [:h3 {:class "text-xl font-semibold text-center text-gray-800"}
            (str/replace prompt-text #"[A-Za-z0-9_-]+\.(png|jpg|jpeg|gif)\?" "")]
           (when-let [correct? @state/last-answer-correct?]
             [:div {:class (str "p-3 text-lg font-bold text-white rounded-lg mb-4 shadow-md "
                                (if correct? "bg-green-500" "bg-red-500"))}
              (if correct? "Correct! Well done!" (str "Wrong! Correct answer was: " correct-display))])
           [:div {:class "grid grid-cols-1 gap-4"}
            (for [opt (:options q)]
              (let [is-correct (= opt correct-display)
                    is-wrong (and (false? @state/last-answer-correct?) (= opt chosen) (not is-correct))
                    disabled? (some? @state/last-answer-correct?)

                    button-class (cond-> "w-full py-4 px-6 text-lg font-medium rounded-xl transition-all duration-200 shadow-md transform hover:scale-[1.02]"
                                   (not disabled?) (str " bg-white text-gray-800 border-2 border-indigo-200 hover:bg-indigo-100 ")
                                   disabled? (str " cursor-not-allowed ")
                                   (and disabled? is-correct) (str " !bg-green-500 !text-white !shadow-lg !shadow-green-300 ")
                                   (and disabled? is-wrong) (str " !bg-red-500 !text-white !shadow-lg !shadow-red-300 ")
                                   (and disabled? (not is-correct) (not is-wrong)) (str " opacity-50 bg-gray-100 text-gray-500"))]
                ^{:key opt}
                [:button {:on-click #(api/submit-answer opt)
                          :disabled disabled?
                          :class button-class}
                 opt]))]])))))