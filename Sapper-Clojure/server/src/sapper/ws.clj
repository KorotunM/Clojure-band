(ns sapper.ws
  (:require
   [org.httpkit.server :as http]
   [cheshire.core :as json]
   [clojure.core.async :refer [put!]]
   [sapper.state :refer [clients event-chan broadcast-state!]]))

(defn ws-handler [req]
  (http/with-channel req channel
    (swap! clients conj channel)
    (println "Player connected!")
    (broadcast-state!) ;; <<< добавь это сразу после подключения игрока
    (http/on-close channel
      (fn [status]
        (println "Player disconnected")
        (swap! clients disj channel)))
    (http/on-receive channel
      (fn [msg]
        (println "[ws] Получено сообщение от клиента:" msg)
        (let [event (json/parse-string msg true)]
          (put! event-chan event))))))
