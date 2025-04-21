(ns sapper.state
  (:require
   [clojure.core.async :refer [chan go-loop <! put!]]
   [cheshire.core :as json]
   [org.httpkit.server :as http]
   [sapper.game :as game]))


;; Общее состояние игры
(def game-state (ref {}))

;; Список клиентов (WebSocket соединений)
(def clients (atom #{}))

;; Канал для обработки событий от игроков
(def event-chan (chan))

(defn broadcast-state! []
  (let [state @game-state
        message (json/generate-string state)]
    (println "[broadcast] Отправка состояния игрокам...")

    (doseq [client @clients]
      (http/send! client message))))


(defn init-game-state []
  (dosync
    (ref-set game-state
      {:board (game/generate-board 20 20 40)
       :revealed #{}
       :flags #{}
       :scores {1 0, 2 0}
       :status :playing
       :time-left 120
       :players []})))

(defn handle-game-event [event]
  (dosync
    (let [{:keys [action player x y]} event]
      (cond
        (= action "open") (alter game-state game/open-cell x y player)
        (= action "flag") (alter game-state game/toggle-flag x y player)
        (= action "restart") (alter game-state (fn [_] (game/new-game 20 20 40)))
        :else nil)))
  (broadcast-state!))

;; Обработчик событий из event-chan
(go-loop []
  (when-let [event (<! event-chan)]
    (handle-game-event event)
    (recur)))
