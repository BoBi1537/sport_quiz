(ns sport-quiz.core
  (:require [reagent.dom.client :as rd]
            [reagent.core :as r]
            [clojure.string :as str]
            [sport-quiz.state :as state :refer [normalize-game-id get-max-time]]
            [sport-quiz.api :as api]
            [sport-quiz.components.classic-quiz :refer [classic-quiz-view]]
            [sport-quiz.components.matching-game :refer [matching-game-view]]))

(defn about-view []
  (let [dark? @state/dark-mode?]
    [:div {:class "space-y-6 animate-in slide-in-from-top-4 duration-300"}
     [:h2 {:class "text-3xl font-black border-b-4 border-indigo-500 pb-2 uppercase italic"} "How to Play"]
     [:div {:class "space-y-5 text-lg leading-relaxed"}
      [:div {:class "flex items-start gap-3"}
       [:span {:class "text-2xl"} "🛡️"]
       [:p [:b {:class "text-indigo-400"} "Equipment Quiz: "] "Identify the sport based on the equipment shown. You have " [:span {:class "font-bold text-red-500"} "10 seconds"] "!"]]
      [:div {:class "flex items-start gap-3"}
       [:span {:class "text-2xl"} "🧩"]
       [:p [:b {:class "text-indigo-400"} "Matching Mania: "] "Match 5 pairs of related sports terms. Connect them all before the " [:span {:class "font-bold text-red-500"} "60s"] " timer runs out!"]]
      [:div {:class "flex items-start gap-3"}
       [:span {:class "text-2xl"} "🏃"]
       [:p [:b {:class "text-indigo-400"} "Athlete Quiz: "] "Name the famous athlete or their sport. You have " [:span {:class "font-bold text-red-500"} "15 seconds"] "!"]]]
     [:button {:on-click #(reset! state/show-about? false)
               :class "w-full mt-6 py-4 bg-gray-500 hover:bg-gray-600 text-white rounded-2xl font-black text-xl transition-all shadow-lg"}
      "GOT IT!"]]))

(defn home-view []
  (let [dark? @state/dark-mode?]
    [:div {:class "text-center space-y-10 py-8 animate-in zoom-in duration-300"}
     [:div {:class (str "mx-auto w-32 h-32 rounded-full flex items-center justify-center text-6xl shadow-2xl "
                        (if dark? "bg-gray-700 ring-4 ring-indigo-500/30" "bg-indigo-50 ring-4 ring-white"))}
      "🏆"]

     [:div {:class "space-y-3"}
      [:h2 {:class "text-4xl font-black uppercase tracking-tighter italic"} "Ultimate Arena"]
      [:p {:class (str "text-xl font-medium " (if dark? "text-gray-400" "text-gray-500"))}
       "Are you a true sports expert?"]]
     [:div {:class "flex flex-col gap-5"}
      [:button {:on-click #(api/start-full-game)
                :class (str "py-6 px-8 rounded-2xl font-black text-3xl shadow-2xl transform hover:scale-105 transition-all uppercase italic tracking-tighter "
                            (if dark? "bg-indigo-600 hover:bg-indigo-500 text-white"
                                "bg-indigo-500 hover:bg-indigo-600 text-white"))}
       "🚀 Start Game"]
      [:button {:on-click #(reset! state/show-about? true)
                :class (str "py-4 px-8 rounded-2xl font-black text-xl border-4 transition-all uppercase tracking-widest "
                            (if dark? "border-gray-700 hover:bg-gray-800 text-white"
                                "border-gray-100 hover:bg-gray-50 text-gray-700"))}
       "📖 About"]]]))

(defn app []
  (fn []
    (let [l-state @state/game-state
          dark? @state/dark-mode?
          show-about? @state/show-about?
          game-id-k (normalize-game-id (:game-id l-state))
          display-total-score (+ (or @state/total-score 0) (or (:score l-state) 0))]
      [:div {:class (str "min-h-screen flex items-center justify-center p-4 sm:p-8 font-sans transition-colors duration-500 "
                         (if dark? "bg-gray-950" "bg-slate-200"))
             :style {:background-image "radial-gradient(circle at 2px 2px, rgba(99, 102, 241, 0.15) 1px, transparent 0)"
                     :background-size "40px 40px"}}
       [:div {:class (str "w-full max-w-xl p-8 sm:p-12 rounded-[3rem] shadow-2xl transition-all duration-500 "
                          (if dark? "bg-gray-900 text-white" "bg-white text-gray-800"))}
        [:div {:class "flex justify-between items-center mb-12"}
         [:button {:on-click #(swap! state/dark-mode? not)
                   :class (str "p-4 rounded-2xl transition-colors text-xl " (if dark? "bg-gray-800" "bg-gray-100"))}
          (if dark? "🌙" "☀️")]
         [:h1 {:class (str "text-4xl font-black italic tracking-tighter "
                           (if dark? "text-indigo-400" "text-indigo-600"))} "SPORT-IQ"]
         [:div {:class "w-12"}]]
        (cond
          show-about? [about-view]
          (nil? l-state) [home-view]
          (:completed? l-state)
          [:div {:class "text-center space-y-8 animate-in bounce-in duration-700"}
           [:div {:class "text-8xl"} "🏁"]
           [:h2 {:class "text-4xl font-black text-green-500 uppercase italic"} "Adventure Done!"]
           [:div {:class "space-y-1"}
            [:p {:class "text-6xl font-black text-indigo-500"} (str display-total-score)]
            [:p {:class "text-xl font-bold opacity-50 uppercase tracking-widest"} "Total XP Points"]]
           [:button {:on-click #(do (reset! state/game-state nil) (reset! state/total-score 0))
                     :class "w-full bg-indigo-500 hover:bg-indigo-600 text-white py-5 rounded-3xl font-black text-2xl transition-all shadow-xl"}
            "PLAY AGAIN"]]
          :else
          [:div {:class "space-y-6"}
           [:div {:class "flex justify-between items-end mb-4"}
            [:div
             [:p {:class "text-xs font-black uppercase opacity-40 tracking-widest"} "Mode"]
             [:h2 {:class "text-2xl font-black text-indigo-400 italic"}
              (str/capitalize (name (or game-id-k "")))]]
            [:div {:class "text-right"}
             [:p {:class "text-xs font-black uppercase opacity-40 tracking-widest"} "Level Score"]
             [:div {:class "text-2xl font-black text-indigo-500"} (str (:score l-state) " / " (:total l-state))]]]
           (case game-id-k
             :matching [matching-game-view]
             :equipment [classic-quiz-view]
             :athlete [classic-quiz-view]
             [:div "Error"])
           [:div {:class (str "mt-10 flex justify-between items-center border-t-4 pt-8 "
                              (if dark? "border-gray-800" "border-gray-50"))}
            [:span {:class "text-lg font-black opacity-40 uppercase tracking-widest"} "Total XP"]
            [:span {:class "text-4xl font-black text-indigo-500 italic"} display-total-score]]])]])))

(defn ^:export init []
  (let [tailwind-script (js/document.createElement "script")]
    (set! (.-src tailwind-script) "https://cdn.tailwindcss.com")
    (.appendChild (.-head js/document) tailwind-script))
  (let [container (.getElementById js/document "app")]
    (when (nil? @state/app-root)
      (reset! state/app-root (rd/create-root container)))
    (.render @state/app-root (r/as-element [app]))))