(ns sample.core
  (:use resolution.core)
  (:import
    (java.awt Toolkit)
    (java.awt Graphics2D)
    (javax.imageio ImageIO)
    (java.io File)
    (java.awt.image BufferedImage)
   ))

(def window-size 500)

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn transpose [a]
  (apply map vector a))

(def map1
  (strs-to-vec
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000001000000100000"
    "000000000000000001000000100000"
    "000000000111111111111111100000"
    "000000000000010001000000100000"
    "000000000000000001000000100000"
    "000000000000000001000000100000"
    "000000000000000001000000100000"
    "000000000000000001000000100000"
    "000000000000000001000000100000"
    "000000000000000001000000100000"
    "000000000000000001000000000000"
    "000000000000000001000000000000"
    "000000000000000001000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"))

;;;; This is a sample project intended to show how to use Resolution.
 
;; initialization and update of state have really similar parts. I wonder if they can
;; be joined together somehow.
 
(defn is-wall? [id]
  (or (= id \1)))

(defn touches-wall? [position map]
  (let [{pos-x :x pos-y :y} position
        pos-x (quot pos-x 20)
        pos-y (quot pos-y 20)]
    (some identity (for [x (range pos-x (+ pos-x 2))
                         y (range pos-y (+ pos-y 2))]
                     (is-wall? (get-in map [x y]))))
    ))

(def speed 13)

(defn sign[n] (cond (> n 0) 1 (< n 0) -1 :else 0))
(defn abs[n] (if (> n 0) n (- n)))

(defn point [x y] {:x x :y y})
(defn add-pt [p1 p2]
  {:x (+ (:x p1) (:x p2))
   :y (+ (:y p1) (:y p2))
   })

(defn point-range [p1 p2]
  (let
      [x1 (:x p1) x2 (:x p2) xsign (sign (- x2 x1))
       y1 (:y p1) y2 (:y p2) ysign (sign (- y2 y1))]
    (cond
      (== x1 x2) (for [y (range y1 y2 ysign)] (point x1 y))
      (== y1 y2) (for [x (range x1 x2 xsign)] (point x y1))
      :else (do
              (assert (== (abs (- x1 x2)) (abs (- y1 y2)))) 
              (for [[x y] (map vector (range x1 x2 xsign) (range y1 y2 ysign))] (point x y))))))

(defn no-collide [obj] (not (touches-wall? obj map1)))

(defn end-game[]
  (println "GAME OVER."))

(def sprites
  (load-spritesheet "src/sample/tiles.png" 20))

(defn render-tile [x-abs y-abs gfx type]
  (case type
    \0 (.setColor gfx (java.awt.Color/WHITE))
    \1 (.setColor gfx (java.awt.Color/BLACK))
    \2 (.setColor gfx (java.awt.Color/RED)))
  (.fillRect gfx x-abs y-abs 20 20))

(defn render-map [gfx map]
  (.setColor gfx (java.awt.Color/RED))

  (dorun (for [x (range 30)
               y (range 30)]
           (render-tile (* x 20) (* y 20) gfx (get-in map [x y])))))

(defn render-game [gfx state]
  (render-map gfx map1) ;;special case! (TODO)
  (doseq [[k v] (seq state)] ((:render v) v gfx))
  state)

(defn bound [x]
  (cond
    (< x 20) 20
    (> x 400) 400
    :else x))

(defn is-on-ground [object]
  (let [feet-pos (point-range {:x (:x object) :y (+ 2 (:y object))} {:x (+ (:x object) 20) :y (+ (:y object) 2)})]
    (some not (map no-collide feet-pos))))

(def JUMP 38)

(defn update-player [object state]
  (let [{x :x y :y vy :vy} object ;;there's a nicer way to do this - forget it at the moment tho.
        on-ground (is-on-ground object)
        dy (if on-ground (if (key-down? JUMP) -20 0) (+ vy 2))
        dx (+ (if (key-down? 39)  speed 0) (if (key-down? 37) (- speed) 0))
        d-pt-x {:x dx :y 0}
        d-pt-y {:x 0 :y dy}
        ]
    (-> object
        ;; update position
        (#(or (last (filter no-collide (point-range % (add-pt d-pt-x %)))) %))
        (#(or (last (filter no-collide (point-range % (add-pt d-pt-y %)))) %))
        (update-in [:x] bound)
        (update-in [:y] bound)
        (assoc-in [:on-ground] true)
        (assoc-in [:vy] dy)
        ;; merge back into object
        (#(merge object %)))))

(defn render-player [player gfx]
  (render-tile (:x player) (:y player) gfx \1))

(defn player[]
  {:x 50
   :y 50
   :vy 0
   :on-ground false
   :render #'render-player
   :update #'update-player
   :depth 5
   :color 'red
   :type :player
  })

(defn healthbar-render [bar gfx]
  (let [{:keys [test x y width height border-color -color good-color -color bad-color health health-max]} bar]
    (let [good-width (* width (/ health health-max))]
       ;;draw colors
       (.setColor gfx bad-color)
       (.fillRect gfx x y width height)
       (.setColor gfx good-color)
       (.fillRect gfx x y good-width height)
       ;;draw border
       (.setColor gfx border-color)
       (.drawRect gfx x y width height)
       )))

(defn healthbar []
  {:x 80
   :y 100
   :health 5
   :health-max 10
   :width 50
   :height 6
   :good-color java.awt.Color/GREEN
   :bad-color java.awt.Color/RED
   :border-color java.awt.Color/BLACK
   :depth 10
   :update (fn [x y] x)
   :render #'healthbar-render
})

(defn enemy-render [enemy gfx]
  (render-tile (:x enemy) (:y enemy) gfx \2))

(defn random-movement []
  (if (< (rand) 0.8)
    0
    (if (> (rand) 0.5) -20 20)))

;; The following three functions involve the new-item queue.
;; Threading a world state through each update function is clearly suboptimal, so we push into the queue
;; messages about anything that would update the world.

(def *queue* (ref {})) ;;:new-items [(make-bullet)]}))

(defn clear-queue [process-msg]
  (let [q @*queue*]
    (reduce merge (flatten (for [key q]
                      (for [msg (key q)] (process-msg msg)))))))

(defn enqueue [type message]
  (dosync (alter *queue* update-in [type] message)))

(defn dist [a b]
   (+ (abs (- (:x a) (:x b)))
      (abs (- (:y a) (:y b)))))

(defn touching? [a b]
  (< (dist a b) 40))

(defn get-pixel [uint]
  [(bit-and (bit-shift-right uint 16) 0xff)
   (bit-and (bit-shift-right uint  8) 0xff)
   (bit-and                  uint     0xff)])

(defn load-map [filename]
  (let [img (ImageIO/read (new File filename))
        width (.getWidth img)
        height (.getHeight img)]
    (for [x (range width)]
      (for [y (range height)]
        (get-pixel (.getRGB img x y))))))

;; take a random step in the dir direction
(defn random-step [obj dir]
  (merge obj {dir (+ (dir obj) (random-movement))}))
  
;;todo - should probably throw if there's more than one.
(defn get-type [col type]
  (if-let [obj (first (filter #(let [[key val] %] (= (:type val) type)) col))]
    (second obj)
    (assert false)))

(defn enemy-update [enemy state]
  ;(println (get-type state :player))
  
  (when (touching? (get-type state :player) enemy)
    (println "ouch"))

  (-> enemy
      (update-in [:x] #(+ % (random-movement)))
      (update-in [:y] #(+ % (random-movement)))
      (update-in [:x] bound)
      (update-in [:y] bound)))

(defn enemy[]
  {:x 220
   :y 200
   :color java.awt.Color/RED
   :update #'enemy-update
   :render #'enemy-render
})

(defn zip-with-times-seen [list]
  ; ['a 'b 'c 'c 'c] => [['a 0] ['b 0] ['c 0] ['c 1] ['c 2]]
  (apply concat (for [a (set list)] (map vector (repeat a) (range (count (filter #(= % a) list)))))))

;a lil better?
;(reduce (fn [acc val] (reduce [] (conj acc [val (count (filter #(= val (first %))))]))))

(defn gen-uniq-names [& fns]
  (let [uniq-vecs (zip-with-times-seen fns)]
    (zipmap (map #(apply str %) uniq-vecs) (map #((first %)) uniq-vecs))))

(defn init []
  (gen-uniq-names #'player #'healthbar #'enemy #'enemy))

(def initial-state (ref {}))

(defn check-fn []
  (if (= @initial-state (#'init))
    {}
    (let [newstate (#'init)
          old @initial-state
          diff (into {} (for [[k val] newstate :when (not (= (newstate k) (old k)))] [k val]))]
      (dosync (ref-set initial-state (#'init)))
      diff)))

(defn update-state [state]
  (map-hash (fn [key value] ((:update value) value state)) state))

;; The #' is for playing nice with the REPL. Sends var, not actual obj
(defn x []
  (res-start
    { :init-fn #'init
      :check-fn #'check-fn
      :update-fn #'update-state
      :render-fn #'render-game}))

(x)


