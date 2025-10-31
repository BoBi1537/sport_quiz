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

(defonce sessions (atom {}))

(defn gen-session-id []
  (str (java.util.UUID/randomUUID)))

(defn safe-get-session [sid]
  (get @sessions sid))

(def games
  {:equipment equipment/equipment-game
   :athlete athlete/athlete-game
   :matching matching/matching-game})

(defn do-start [{:keys [game n] :or {n 5}}]
  (let [g (games (keyword game))]
    (if (nil? g)
      {:error "Unknown game id"}
      (let [sid (gen-session-id)
            state (se/start-game g n)]
        (swap! sessions assoc sid state)
        {:session-id sid
         :state (se/api-state state)}))))

(defn do-answer [{:keys [session-id answer]}]
  (if-let [st (safe-get-session session-id)]
    (let [current-raw-q (nth (:raw-questions st) (:current-index st))
          {:keys [correct? new-state]} (se/submit-answer st answer)
          answer-display (if correct?
                           nil
                           (:answer current-raw-q)) 
          ]
      (swap! sessions assoc session-id new-state)
      {:correct? correct?
       :correct-answer answer-display 
       :state (se/api-state new-state)})
    {:error "Unknown session-id"}))

(defroutes routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (POST "/api/start" req
    (let [res (do-start (:body req))]
      {:status (if (:error res) 400 200)
       :body res}))
  (POST "/api/answer" req
    (let [res (do-answer (:body req))]
      {:status (if (:error res) 400 200)
       :body res}))
  (GET "/api/session/:id" [id]
    (if-let [st (safe-get-session id)]
      {:status 200 :body {:session-id id :state (se/api-state st)}}
      {:status 404 :body {:error "Not found"}}))
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