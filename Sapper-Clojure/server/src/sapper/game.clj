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
                         :flagged false}))))

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
  (let [{p1 1 p2 2} (:scores state)]
    (cond
      (> p1 p2) 1
      (> p2 p1) 2
      :else nil)))

;; Открытие клетки
(defn open-cell [state x y player]
  (let [board (:board state)
        cell (get-in board [x y])]
    (if (or (:opened cell) (:flagged cell))
      state
      (let [adj (count-adjacent-mines board x y)
            opened-board (assoc-in board [x y :opened] true)
            updated-board (assoc-in opened-board [x y :adjacent] adj)
            new-state (assoc state :board updated-board)]
        (cond
          (:mine cell) (assoc new-state :status :ended :winner (if (= player 1) 2 1))
          (game-complete? new-state) (assoc new-state :status :ended :winner (calculate-winner new-state))
          :else new-state)))))

;; Установка/снятие флага
(defn toggle-flag [state x y player]
  (let [board (:board state)
        cell (get-in board [x y])]
    (if (:opened cell)
      state
      (let [flagged (not (:flagged cell))
            updated-board (assoc-in board [x y :flagged] flagged)
            flagged-state (assoc state :board updated-board)]
        (update-score flagged-state x y player)))))

(defn new-game
  "Создаёт новое состояние игры с заданными параметрами"
  [rows cols n-mines]
  {:board (generate-board rows cols n-mines)
   :scores {1 0, 2 0}
   :status :playing
   :winner nil
   :time-left 300}) 


