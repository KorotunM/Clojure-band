(ns sapper.game
  (:require [clojure.set :as set]))

;; Подсчёт мин вокруг клетки
(defn count-adjacent-mines [board x y]
  (let [rows (count board)
        cols (count (first board))
        neighbors (for [dx [-1 0 1]
                        dy [-1 0 1]
                        :let [nx (+ x dx) ny (+ y dy)]
                        :when (and (not (and (= dx 0) (= dy 0))) ; исключаем саму клетку
                                   (>= nx 0) (< nx rows)
                                   (>= ny 0) (< ny cols))]
                    (get-in board [nx ny]))]
    (count (filter :mine neighbors))))

;; Генерация игрового поля
(defn generate-board
  [rows cols n-mines]
  (let [total-cells (* rows cols)
        mine-positions (set (take n-mines (shuffle (range total-cells))))
        mine? #(contains? mine-positions %)
        to-pos (fn [x y] (+ (* x cols) y))]

    (let [initial-board
          (vec (for [x (range rows)]
                 (vec (for [y (range cols)]
                        {:x x
                         :y y
                         :mine (mine? (to-pos x y))
                         :adjacent 0
                         :opened false
                         :flagged false
                         :flagged-by nil}))))

          updated-board
          (vec (for [x (range rows)]
                 (vec (for [y (range cols)]
                        (let [adj (count-adjacent-mines initial-board x y)]
                          (assoc (get-in initial-board [x y]) :adjacent adj))))))]
      updated-board)))




;; Проверка окончания игры
(defn game-complete? [state]
  (let [board (:board state)
        all-cells (apply concat board)
        unopened (filter #(not (:opened %)) all-cells)
        all-mines (every? :mine unopened)]
    all-mines))

;; Подсчёт очков игрока
(defn update-score [state x y player]
  (let [cell (get-in (:board state) [x y])]
    (if (and (:flagged cell) (:mine cell))
      (update-in state [:scores player] inc)
      state)))

;; Проверка победителя по очкам
(defn calculate-winner [state]
  (let [scores (:scores state)]
    ;; если есть хотя бы один игрок – берём entry [id score] с максимальным score
    (when (seq scores)
      (key (apply max-key val scores)))))

;; Открытие клетки
(defn open-cell [state x y player]
  (let [board (:board state)
        cell  (get-in board [x y])]
    (if (or (:opened cell) (:flagged cell))
      state
      (let [adj           (count-adjacent-mines board x y)
            opened-board  (assoc-in board [x y :opened] true)
            updated-board (assoc-in opened-board [x y :adjacent] adj)
            new-state     (assoc state :board updated-board)]
        (cond
           ;; Взрыв: отмечаем, кто подорвался, сбрасываем ему очки и завершаем
            (:mine cell)
              (-> new-state
                  (assoc :status   :ended
                         :winner   (calculate-winner new-state)
                         :exploded player)
                  (assoc :scores  (assoc (:scores new-state) player 0)))

          ;; если открыли всё – аналогично
          (game-complete? new-state)
            (assoc new-state
                   :status :ended
                   :winner (calculate-winner new-state))

          :else
            new-state)))))

;; Установка/снятие флага
(defn toggle-flag [state x y player]
  (let [board (:board state)
        cell  (get-in board [x y])
        opened? (:opened cell)
        owner   (:flagged-by cell)]
    (cond
      ;; Нельзя ставить/снимать флаг на уже открытой клетке
      opened?
      state

      ;; Если флаг не стоит — ставим его от имени player
      (nil? owner)
      (let [updated-cell  (-> cell
                              (assoc  :flagged true
                                      :flagged-by player))
            updated-board (assoc-in board [x y] updated-cell)
            new-state     (assoc state :board updated-board)]
        (update-score new-state x y player))

      ;; Если флаг стоит и его автор — тот же игрок — снимаем
      (= owner player)
      (let [updated-cell  (-> cell
                              (assoc :flagged false
                                     :flagged-by nil))
            updated-board (assoc-in board [x y] updated-cell)]
        (assoc state :board updated-board))

      ;; Если флаг стоит, но автор другой — игнорируем
      :else
      state)))

(defn new-game
  "rows cols mines, и список player-IDs"
  ([rows cols mines players]
   {:board     (generate-board rows cols mines)
    :revealed  #{}
    :flags     #{}
    :scores    (into {} (map (fn [pid] [pid 0]) players))
    :status    :playing
    :time-left 120
    :players   players})
  ;; для обратной совместимости, если кто-то вызывает new-game без списка
  ([rows cols mines]
   (new-game rows cols mines [1 2])))



