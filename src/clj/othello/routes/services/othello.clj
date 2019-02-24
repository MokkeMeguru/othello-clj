(ns othello.routes.services.othello)

;;;;;;;;;;;;;;;;;;;;;;;;;
;;     BOARD IMAGE     ;;
;;                     ;;
;;   - - - - - - - -   ;;
;;   - - - - - - - -   ;;
;;   - - - - - - - -   ;;
;;   - - - o x - - -   ;;
;;   - - - x o - - -   ;;
;;   - - - - - - - -   ;;
;;   - - - - - - - -   ;;
;;   - - - - - - - -   ;;
;;                     ;;
;; * begin at 0        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

;; GAME INFORMATION ;;
(def BOARD-SIZE 8)
(def BOARD-HEXSIZE (* BOARD-SIZE BOARD-SIZE))
(def player1 1)
(def player2 -1)
(def emp 0)
(def minimax-depth 3)

;;;;;;;;;;;;;;;;;;;;;;

;; utilities for position ;;
(defn move [key pos board-size]
  (case key
    ::upleft (dec (move ::up pos board-size))
    ::up (- pos board-size)
    ::upright (inc (move ::up pos board-size))
    ::right (inc pos)
    ::downright (inc (move ::down pos board-size))
    ::down (+ pos board-size)
    ::downleft (dec (move ::down pos board-size))
    ::left (dec pos)
    (throw (Exception. "got an unknown order"))))

(defn movable? [key pos board-size]
  (case key
    ::upleft (and (movable? ::up pos board-size) (movable? ::left pos board-size))
    ::up (<= board-size pos)
    ::upright (and (movable? ::up pos board-size) (movable? ::right pos board-size))
    ::right (not= (mod (inc pos) 8) 0)
    ::downright (and (movable? ::down pos board-size) (movable? ::right pos board-size))
    ::down (>= (* board-size (dec board-size)) pos)
    ::downleft (and (movable? ::down pos board-size) (movable? ::left pos board-size))
    ::left (not= (mod pos 8) 0)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; utilitiies for flip ;;

(defn search-flippable-stone-with-direction
  ([key pos board-size player board]
   (search-flippable-stone-with-direction
    key pos board-size player board '()))
  ([key pos board-size player board acc]
   (if (movable? key pos board-size)
     (let [next-pos (move key pos board-size)]
       (condp = (get board next-pos)
         (* -1 player) (search-flippable-stone-with-direction
                        key next-pos board-size player board (cons next-pos acc))
         player acc
         '()))
    '())))

(defn search-flippable-stone
  [pos board-size player board]
  (if (some (partial = (get board pos)) [player1 player2])
    '()
    (mapcat (fn [direction]
                (let [flippable-stone
                      (search-flippable-stone-with-direction
                       direction pos board-size player board)]
                  (when-not (= '() flippable-stone)
                    flippable-stone)))
              '(::upleft ::up ::upright ::right ::downright ::down ::downleft ::left))))

(defn reflect-flip [put-stone-pos order player board]
  (assert (not= order '()))
  (loop [_board board
         _order (cons put-stone-pos order)]
    (if-not (= '() _order)
      (recur (assoc _board (first _order) player) (rest _order))
      _board)))
;;;;;;;;;;;;;;;;;;;;;;;;;

;; utilitiies for searching effective points ;;

(defn eval-state [board]
  (reduce +
          board
          ;; (concat (filter (partial = 1) board)
          ;;         (filter (partial = -1) board))
          ))

(defn search-all-strategy [board-size player board]
  (remove nil?
          (map #(let [_strategy (search-flippable-stone % board-size player board)]
                  (if (= _strategy '())
                   nil
                   (let [_board (reflect-flip % _strategy player board)]
                       {:pos %
                        :strategy _strategy
                        :board _board
                        :turn player
                        })))
               (range (* board-size board-size)))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; utilities for generate game tree ;;

;; TODO: memorize at board, depth, player
(defn calc-negmax [board depth board-size player]
  (let [_this-score (* player (eval-state board))]
    (if (zero? depth)
      _this-score
      (let [_strategies (search-all-strategy board-size player board)]
        (if (= _strategies '())
          _this-score
          (+ _this-score
             (* -1
                (apply min
                       (map
                        #(calc-negmax (:board %) (dec depth) board-size (* -1 player))
                        _strategies)))))))))

(defn find-best-pos [board depth board-size player]
  (let [_strategies (search-all-strategy board-size player board)]
    (if (= _strategies '())
      {:result (eval-state board)}
      (let [_eval-score (pmap
                         #(assoc % :eval-score
                                 (calc-negmax
                                  (:board %) depth board-size player)) _strategies)
            _sorted-eval-score (sort-by :eval-score > _eval-score)
            _max-score (-> _sorted-eval-score first :eval-score)]
        (rand-nth (filter #(= _max-score (:eval-score %)) _sorted-eval-score))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; utilitiies for CUI ;;

(defn print-board [board board-size]
  (loop [_board board
         _text ""]
    (if (>= (count _board) board-size)
      (let [row (take board-size _board)]
        (recur (drop board-size _board)
               (str _text
                    (apply str
                           (map #(condp =  % emp "- " player1 "o " player2 "x " "e ") row))
                    "\n")))
      (str "*Board*\n" _text))))
;;;;;;;;;;;;;;;;;;;;;;;;;

;; utilities for game loop ;;
(defn input-loop [board-size player board]
  (loop []
    (let [_input (read-string (read-line))]
      (if (int? _input)
        (if (and (<= 1 _input)
                 (<= _input 64))
          (let [_flippable-stone (search-flippable-stone
                                  (dec _input) board-size player board)]
            (if (= '() _flippable-stone)
              (recur)
              (reflect-flip (dec _input) _flippable-stone player board)))
          (recur))
        (if (= "quit" _input)
          (recur))))))

(defn game-loop [init-board minimax-depth board-size init-player]
  (loop [_board init-board
         _player init-player]
    (print (print-board _board board-size))
    (let [_strategies (search-all-strategy board-size _player _board)]
      (print (map #(inc (:pos %)) _strategies) "\n")
      (if (= '() _strategies)
        (print (if (pos? (eval-state _board)) "Win" "Lose"))
        (if (= _player 1)
          (when-let [__board (input-loop board-size _player _board)]
           (recur __board
                  (* -1 _player)))
          (recur (:board (find-best-pos _board minimax-depth board-size _player))
                 (* -1 _player)))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; utilities for api ;;
;; TODO: fix find-best-pos -> use fn:search-all-strategy
;; TODO:
(defn put-stone [input board]
  (let [_flippable-stone (search-flippable-stone input BOARD-SIZE player1 board)]
    (if (= '() _flippable-stone)
      {:message "invalid position" :result board}
      (let [_flipped-board (reflect-flip input _flippable-stone player1 board)
            _next-strategies (search-all-strategy BOARD-SIZE player2 _flipped-board)]
        (if (= '() _next-strategies)
          (if (= '() (filter #(= 0 %) _flipped-board))
            {:message (if (pos? (eval-state _flipped-board)) "YOU WIN" "YOU LOSE")
             :result _flipped-board}
            {:message "PASS"
             :result _flipped-board})
          (loop [_next-board (:board
                              (find-best-pos
                               _flipped-board minimax-depth BOARD-SIZE player2))
                 _stacked-board '()]
            (if (and (not= '()  (filter #(= 0 %) _next-board))
                     (= '() (search-all-strategy BOARD-SIZE player1 _next-board)))
              (recur (:board
                      (find-best-pos
                       _next-board minimax-depth BOARD-SIZE player2))
                     (cons _next-board _stacked-board))
              ;; {:message (if (pos? (eval-state _flipped-board)) "YOU WIN" "YOU LOSE")
              ;;  :result _flipped-board}
              (if (= '() (filter #(= 0 %) _next-board))
                {:message (if (pos? (eval-state _next-board)) "YOU WIN" "YOU LOSE")
                 :result _next-board
                 :stacked-board _stacked-board}
                {:message "accept"
                 :result _next-board
                 :pre-result _flipped-board
                 :stacked-board _stacked-board
                 :can-put (map
                           #(:pos %)
                           (search-all-strategy
                            BOARD-SIZE player1 _next-board))}))))))))

;; {
;;  "input" : 62,
;;  "board" : 
;;  [1,1,1,1,1,1,1,1,
;;   1,1,-1,-1,1,-1,1,-1,
;;   1,1,1,1,-1,1,-1,-1,
;;   1,1,1,1,-1,1,-1,-1,
;;   1,-1,1,1,-1,1,-1,-1,
;;   1,1,1,1,-1,-1,-1,-1,
;;   1,1,1,1,-1,-1,-1,-1,
;;   -1,-1,-1,1,-1,0,0,-1]
;;  }

;;;;;;;;;;;;;;;;;;;;;;;

;; initialization ;;
(defn set-initial-place [board-size white-board]
  (let [top-left-pos (dec (- (* board-size (/ board-size 2)) (/ board-size 2)))
        prefix (list [0 0] [0 1] [1 0] [1 1])
        calc-prefix (fn [[x y]] (+ top-left-pos x (* board-size y)))]
    (loop [_prefix prefix
           _white-board white-board]
      (if-let [_p (first _prefix)]
        (recur (rest _prefix)
               (assoc _white-board
                      (calc-prefix _p)
                      (if (= 0 (mod (reduce + _p) 2)) player1 player2)))
        _white-board))))

(defn gen-board [board-size]
  (let [board-hexsize (* board-size board-size)
        white-board (vec (repeat board-hexsize 0))]
    (set-initial-place board-size white-board)))
;;;;;;;;;;;;;;;;;;;;;;
