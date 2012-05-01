(ns sample.core
  (:use resolution.core)
  (:import
    (java.awt Toolkit)
    (java.awt Graphics2D)
   ))

(def window-size 500)

(def map1
  (strs-to-vec
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000000000000000000000000"
    "000000000111111111111111000000"
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
 
;; TODO: type is a redundancy here. (with res-init)
 
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

(def speed 5)

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

(defn update-state-old [old-state]
  ;;these multimethods must be defined inside res-update in order to gain
  ;; closures over state etc. 

  ;; I think this is bad style, should probably be passing those in.
   
 

 (defmulti update-object :type)
 
 ;;38 up ;;39 right
 ;;40 down ;;
 ;;TODO i am hardcoding the current map in this method. take it out!
 (defmethod update-object :player [object]
   (let [{x :x y :y} object
         dx (+ (if (key-down? 39)  speed 0) (if (key-down? 37) (- speed) 0))
         dy (+ (if (key-down? 38) (- speed) 0) (if (key-down? 40)  speed 0))
         d-pt-x {:x dx :y 0}
         d-pt-y {:x 0 :y dy}]
     (-> object
         (#(or (last (filter no-collide (point-range % (add-pt d-pt-x %)))) %))
         (#(or (last (filter no-collide (point-range % (add-pt d-pt-y %)))) %))
         (merge {:type :player}))))

 (defmethod update-object :color [object] {:color 'white :type :color})
 (defmethod update-object :text [object] object)
 
 (defn new-objects [old-state]
   {})
  
  ; map can update extant items and remove them, but not add.
  ; for some odd reason i cannot think of a better way to solve this.

 (merge
   (map-hash (fn [key value] (update-object value)) old-state)
   (new-objects old-state)))
 
(defn end-game[]
  (println "GAME OVER."))

(def sprites (load-spritesheet "src/sample/tiles.png" 20))

(defn render-tile [x-abs y-abs gfx type]
  (if (= type \0)
    (.setColor gfx (java.awt.Color/WHITE)))
  (if (= type \1)
    (.setColor gfx (java.awt.Color/BLACK)))
  (.fillRect gfx x-abs y-abs 20 20))

(defn render-map [gfx map]
  (.setColor gfx (java.awt.Color/RED))

  (dorun (for [x (range 30)
               y (range 30)]
           (render-tile (* x 20) (* y 20) gfx (get-in map [x y]))
           )))

(defn render-game [gfx state]
  (render-map gfx map1)

  (defn render-player [object gfx]
    (draw-sprite sprites [1 0] gfx [(:x object) (:y object)]))

  (defn render-textbox [object gfx]
    (.setColor gfx (java.awt.Color/RED))
    (.drawString gfx (:content object) (:x object) (:y object)))

  ;;(defn render-object [object gfx]
   ;; (cond
   ;;   (= (:type object) :player) (render-player object gfx)
   ;;   (= (:type object) :text) (render-textbox object gfx)))

  (doseq [[k v] state] ((:render v) v gfx))
  state
  
  ;; example sprite usage
  ;; (draw-sprite sprites [0 0] gfx [350 350])
)

 (defn update-player [object]
   (let [{x :x y :y} object
         dx (+ (if (key-down? 39)  speed 0) (if (key-down? 37) (- speed) 0))
         dy (+ (if (key-down? 38) (- speed) 0) (if (key-down? 40)  speed 0))
         d-pt-x {:x dx :y 0}
         d-pt-y {:x 0 :y dy}]
     (->> object
         (#(or (last (filter no-collide (point-range % (add-pt d-pt-x %)))) %))
         (#(or (last (filter no-collide (point-range % (add-pt d-pt-y %)))) %))
         (merge object))))

;(defmacro localize [obj keys]
;  (list 'let 

(defn player[]
  {:x 50
   :y 50
   :render #(draw-sprite sprites [1 0] %2 [(:x %1) (:y %1)])
   :update #'update-player
   :depth 5
   :color 'red
   :type :player
  })

(defn healthbar-render [bar gfx]
  (let [{test :test x :x y :y width :width height :height border-color :border-color good-color :good-color bad-color :bad-color health :health health-max :health-max} bar]
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
  {:x 60
   :y 80
   :test 55
   :health 5
   :health-max 10
   :width 50
   :height 10
   :good-color java.awt.Color/GREEN
   :bad-color java.awt.Color/RED
   :border-color java.awt.Color/BLACK
   :depth 10
   :update (fn [x] x)
   :render #'healthbar-render
})

(defn init []
  { :player (player)
    :bar (healthbar)
    :test (player)
    :test2 (player)
    ;;:background-color {:color 'white :type :color}
  })


(def initial-state (ref {}))

(defn eql-but-functions [a b]
  (cond
    (or (nil? a) (nil? b)) false
    (and (fn? a) (fn? b)) true
    (map? a) (and (every? (fn [[k v]] (eql-but-functions (a k) (b k))) a)
                  (every? (fn [[k v]] (eql-but-functions (a k) (b k))) b))
    :else (= a b)))

(defn check-fn []
  (if (eql-but-functions @initial-state (#'init))
    {}
    (let [newstate (#'init)
          old @initial-state
          diff (into {} (for [[k val] newstate :when (not (eql-but-functions (newstate k) (old k)))] [k val]))]
      (dosync (ref-set initial-state (#'init)))
      diff)))

(defn update-state [state]
  (map-hash (fn [key value] ((:update value) value)) state))

;; The #' is for playing nice with the REPL. Sends var, not actual obj
(defn x []
  (res-start
    { :init-fn #'init
      :check-fn #'check-fn
      :update-fn #'update-state
      :render-fn #'render-game}))

(x)


