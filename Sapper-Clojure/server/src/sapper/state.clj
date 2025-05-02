(ns sapper.state
  (:require
  [clojure.core.async :refer [chan go-loop <! timeout alt! close!]]
   [cheshire.core :as json]
   [org.httpkit.server :as http]
   [sapper.game :as game]))


;; Общее состояние игры
(def game-state (ref {}))

;; Список клиентов (WebSocket соединений)
(def clients (atom #{}))

;; Канал для обработки событий от игроков
(def event-chan (chan))

; канал‑сигнал для остановки таймера
(defonce exit-ch (atom nil))

;; отсановка таймера
(defn stop-timer! []
  (when-let [ch @exit-ch]
    (close! ch)))

(defn broadcast-state! []
  (let [state @game-state
        message (json/generate-string state)]
    ;; (println "[broadcast] Отправка состояния игрокам...")
    (doseq [client @clients]
      (http/send! client message))))

;; Запуск цикла таймера, который каждую секунду уменьшает time-left
(defn start-timer! []
  (let [ch (chan)]
    (reset! exit-ch ch)
    (go-loop []
      (alt!
        ch ([_])
        (timeout 1000) ([_]
          (dosync
            (when (pos? (:time-left @game-state))
              (alter game-state update :time-left dec)))
          (broadcast-state!)
          (if (pos? (:time-left @game-state))
            (recur)
            (do
              (dosync
                (alter game-state
                       assoc
                       :status :ended
                       :winner (game/calculate-winner @game-state)))
              (broadcast-state!))))))))

(defn init-game-state []
  (dosync
    (ref-set game-state
      {:board (game/generate-board 20 20 40)
       :revealed #{}
       :flags #{}
       :scores {1 0, 2 0}
       :status :playing
       :time-left 120
       :players []}))
  (start-timer!))

(defn handle-game-event [event]
  (dosync
    (let [{:keys [action player x y]} event]
      (cond
        (= action "open") (alter game-state game/open-cell x y player)
        (= action "flag") (alter game-state game/toggle-flag x y player)
        (= action "restart") 
        (do
          (when-let [ch @exit-ch] (close! ch))
          (init-game-state))
        :else nil)))
  ;; для остановки таймера при завершении игры 
  ;; (если будет больше 2 ироков, возможно нужно будет изменить)
  (when (= (:status @game-state) :ended)
    (stop-timer!))
  ;; 
  (broadcast-state!))

;; Обработчик событий из event-chan
(go-loop []
  (when-let [event (<! event-chan)]
    (handle-game-event event)
    (recur)))
