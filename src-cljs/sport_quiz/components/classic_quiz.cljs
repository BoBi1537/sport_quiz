(ns sport-quiz.components.classic-quiz
  (:require [clojure.string :as str]
            [sport-quiz.state :as state]
            [sport-quiz.api :as api]
            [sport-quiz.components.common :refer [progress-bar time-remaining-view]]))

(defn classic-quiz-view []
  (fn []
    (let [state-val @state/game-state
          dark? @state/dark-mode?
          q (:current-question state-val)]
      (when q
        (let [chosen @state/last-chosen-answer
              correct-display @state/correct-answer
              is-answered? (some? @state/last-answer-correct?)
              is-actually-correct? @state/last-answer-correct?
              prompt-text (:prompt q)
              img-match (re-find #"[A-Za-z0-9_-]+\.(png|jpg|jpeg|gif)" prompt-text)
              img (if (vector? img-match) (first img-match) img-match)
              time @state/time-remaining]
          [:div {:class "space-y-4"}
           [progress-bar state-val]
           [time-remaining-view time (:game-id state-val)]
           (when img
             [:div {:class "text-center my-4 animate-in fade-in zoom-in duration-500"}
              [:img {:src (str "/images/" img)
                     :class (str "max-h-52 w-auto mx-auto rounded-2xl shadow-lg border-4 "
                                 (if dark? "border-gray-700" "border-white"))
                     :alt "Question"}]])
           [:h3 {:class "text-2xl font-bold text-center leading-tight mb-6"}
            (str/replace prompt-text #"[A-Za-z0-9_-]+\.(png|jpg|jpeg|gif)\?" "")]
           [:div {:class "grid grid-cols-1 gap-3"}
            (for [opt (:options q)]
              (let [is-this-correct-opt (= opt correct-display)
                    is-this-chosen-opt (= opt chosen)
                    button-class (cond-> "w-full py-4 px-6 text-lg font-bold rounded-2xl transition-all duration-300 shadow-sm "
                                   (not is-answered?)
                                   (str (if dark? " bg-gray-700 hover:bg-gray-600 text-white "
                                            " bg-white text-gray-800 border-2 border-gray-100 hover:border-indigo-200 hover:bg-indigo-50 ")
                                        " hover:translate-y-[-2px] ")
                                   is-answered? (str " cursor-not-allowed ")
                                   (and is-answered? is-this-correct-opt)
                                   (str " !bg-green-500 !text-white !scale-105 shadow-green-500/50 ")
                                   (and is-answered? is-this-chosen-opt (not is-actually-correct?))
                                   (str " !bg-red-500 !text-white animate-shake ")
                                   (and is-answered? (not is-this-correct-opt) (not (and is-this-chosen-opt (not is-actually-correct?))))
                                   (str " opacity-40 " (if dark? "bg-gray-800" "bg-gray-50 text-gray-400")))]
                ^{:key opt}
                [:button {:on-click #(api/submit-answer opt)
                          :disabled is-answered?
                          :class button-class}
                 opt]))]])))))