(ns sport-quiz.server
  (:require [org.httpkit.server :as http]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [sport-quiz.state-engine :as se]
            [sport-quiz.games.equipment :as equipment]
            [sport-quiz.games.athlete :as athlete]
            [sport-quiz.games.matching :as matching]
            [sport-quiz.db.core :as db]))

(def games
  {:equipment equipment/equipment-game
   :athlete   athlete/athlete-game
   :matching  matching/matching-game})

(defn do-start [{:keys [game n] :or {n 5}}]
  (let [g (games (keyword game))]
    (if (nil? g)
      {:error "Unknown game id"}
      (se/start-game-db g n))))

(defn do-answer [{:keys [session-id answer]}]
  (let [sid (Integer/parseInt (str session-id))
        res (se/submit-answer-db sid answer)]
    (cond
      (nil? res)
      {:error "Unknown session-id"}

      (:error res)
      {:error "Unknown session-id"}

      :else
      (let [{:keys [correct? raw-question new-state]} res
            correct-answer (if (true? correct?)
                             nil
                             (when (false? correct?)
                               (:answer raw-question)))
            final-correct? (if (nil? correct?) false correct?)]

        {:correct? final-correct?
         :correct-answer correct-answer
         :state new-state}))))

(defroutes routes
  (GET "/" []
    (resp/resource-response "index.html" {:root "public"}))
  
  (POST "/api/start" req
    (let [res (do-start (:body req))]
      {:status (if (:error res) 400 200)
       :body res}))
  (POST "/api/answer" req
    (let [res (do-answer (:body req))]
      {:status (if (:error res) 400 200)
       :body res}))
  (GET "/api/session/:id" [id]
    (let [sid (Integer/parseInt id)]
      (if-let [st (se/get-state-by-id sid)]
        {:status 200
         :body {:session-id sid
                :state (assoc st :current-question nil)}}
        {:status 404 :body {:error "Not found"}})))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> routes
      (wrap-json-body {:keywords? true})
      wrap-json-response
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))

(defn start-server [& {:keys [port] :or {port 3000}}]
  (db/init-db)
  (http/run-server app {:port port}))

(defn -main [& _]
  (start-server)
  (println "Sport Quiz server running on http://localhost:3000"))