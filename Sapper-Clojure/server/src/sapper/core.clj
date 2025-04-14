(ns sapper.core
  (:require
    [org.httpkit.server :as http]
    [compojure.core :refer [GET POST routes]]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [sapper.ws :refer [ws-handler]]
    [sapper.state :refer [init-game-state]])
  (:gen-class))

(defn app []
  (routes
    (GET "/ws" [] ws-handler) ; WebSocket endpoint
    (route/resources "/")      ; отдача статики (build клиента)
    (route/not-found "404 Not Found")))

(defn -main [& args]
  (println "Starting Sapper Game Server on port 8080...")
  (init-game-state) ; инициализация состояния игры
  (http/run-server (wrap-defaults (app) site-defaults) {:port 8080}))
