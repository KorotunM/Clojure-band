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

;; канал‑сигнал для лобби: channel -> player-id
(def client-ids (atom {}))

;; собственно состояние лобби: player-id -> {:nick string :ready boolean}
(defonce lobby-state (atom {}))

; канал‑сигнал для остановки таймера
(defonce exit-ch (atom nil))

;; для нумерации новых игроков
(defonce join-counter (atom 0))

;; отсановка таймера
(defn stop-timer! []
  (when-let [ch @exit-ch]
    (close! ch)))

;; добавляет нового игрока в лобби
(defn add-player! [player-id]
  (swap! lobby-state assoc player-id {:nick player-id :ready false}))

;; удаляет игрока из лобби
(defn remove-player! [player-id]
  (swap! lobby-state dissoc player-id))

;; устанавливает ник игрока
(defn set-player-nick! [player-id nick]
  (swap! lobby-state assoc-in [player-id :nick] nick))

;; переключает готовность
(defn toggle-player-ready! [player-id]
  (swap! lobby-state update-in [player-id :ready] not))

;; раздать всем клиентам текущее лобби
(defn broadcast-lobby! []
  (let [msg (json/generate-string {:type    "lobby/update"
                                   :players @lobby-state})]
    (doseq [ch @clients]
      (http/send! ch msg))))

(defn broadcast-state! []
  (let [msg       {:type  "state"
                   :state @game-state}
        json-msg (json/generate-string msg)]
    (doseq [client @clients]
      (http/send! client json-msg))))

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
  (let [pids (keys @lobby-state)]
    (dosync
      (ref-set game-state
        (game/new-game 20 20 40 pids)))
    (broadcast-state!)
    (start-timer!)))

(defn handle-game-event [event]
  (let [{t :type{:keys [x y player]} :payload} event]
    (dosync
      (case t
        "game/open"
          (alter game-state game/open-cell x y player)
        "game/flag"
          (alter game-state game/toggle-flag x y player)
        "game/restart"
          (do
            (stop-timer!)
            (init-game-state))
        nil)))
  ;; если игра закончилась — останавливаем таймер
  (when (= (:status @game-state) :ended)
    (stop-timer!))
  (broadcast-state!))

;; Обработчик событий из event-chan
(go-loop []
  (when-let [event (<! event-chan)]
    (handle-game-event event)
    (recur)))

(defn try-start-game! []
  (let [ps (vals @lobby-state)
        ready-count (count (filter :ready ps))]
    (when (and (not (:started? @game-state)) ; если ещё не играем
               (>= (count ps) 2)
               (= ready-count (count ps)))
      ;; инициализируем состояние игры и раскатываем это всем
      (stop-timer!)
      (init-game-state)
      (let [msg (json/generate-string {:type "game/start"})]
        (doseq [ch @clients]
          (http/send! ch msg))))))
