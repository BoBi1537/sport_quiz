(ns sport-quiz.frontend
  (:require [reagent.dom :as rd]))

(defn app-root []
  [:div
   [:h1 "Sport Quiz"]
   [:p "Frontend placeholder — Reagent."]])

(defn ^:export init []
  (rd/render [app-root] (.getElementById js/document "app")))