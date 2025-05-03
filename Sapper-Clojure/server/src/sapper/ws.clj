(ns sapper.ws
  (:require
   [org.httpkit.server :as http]
   [cheshire.core :as json]
   [clojure.core.async :refer [put!]]
   [sapper.state :refer [clients client-ids
                         add-player! remove-player!
                         set-player-nick! toggle-player-ready!
                         broadcast-lobby! broadcast-state! try-start-game!
                         event-chan lobby-state broadcast-lobby! join-counter]]))

(defn ws-handler [req]
  (http/with-channel req channel
    ;; 1) генерируем уникальный player-id и порядковый номер для ника
    (let [player-id    (str (hash channel))
          num          (swap! join-counter inc)
          default-nick (str "игрок-" num)]
      ;; 2) сохраняем канал и id
      (swap! clients conj channel)
      (swap! client-ids assoc channel player-id)

      ;; 3) добавляем в лобби и сразу даём дружелюбный ник
      (add-player! player-id)
      (set-player-nick! player-id default-nick)

      ;; 4) шлём только что присоединившемуся его id
      (http/send! channel
        (json/generate-string {:type "lobby/joined" :id player-id}))

      ;; 5) обновляем у всех список лобби и (если нужно) текущее игровое состояние
      (broadcast-lobby!)
      (broadcast-state!)

      ;; 6) при дисконнекте удаляем игрока
      (http/on-close channel
        (fn [_]
          (let [pid (@client-ids channel)]
            (remove-player! pid)
            (swap! clients disj channel)
            (swap! client-ids dissoc channel)
            (broadcast-lobby!))))

      ;; 7) на все входящие сообщения
      (http/on-receive channel
        (fn [raw]
          (let [{type    :type
                 payload :payload} (json/parse-string raw true)
                pid (@client-ids channel)]
            (case type
              ;; лобби‑события
              "lobby/set-nick"
                (do (set-player-nick! pid payload)
                  (broadcast-lobby!))
              "lobby/toggle-ready"
                (do (toggle-player-ready! pid)
                  (broadcast-lobby!)
                  (when (try-start-game!)))
              ;; игровые события
              "game/open"
                (put! event-chan {:type type :payload payload})
              "game/flag"
                (put! event-chan {:type type :payload payload})
              "game/restart"
                (do
                  ;; сброс готовности и уведомление клиентов
                  (swap! lobby-state
                    (fn [l]
                      (into {}
                        (map (fn [[k v]]
                              [k (assoc v :ready false)])
                          l))))
                  (broadcast-lobby!)
                  (doseq [ch @clients]
                    (http/send! ch
                      (json/generate-string {:type "lobby/reset"}))))
              (println "Unknown message type:" type))))))))