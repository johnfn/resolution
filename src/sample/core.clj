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

(defn update-state [old-state]
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
         new-object object
         new-object (or (last (filter (fn[x] (not (touches-wall? x map1)))
                                      (point-range (point x y) (point (+ x dx) (+ y dy))))) object)
         ]
     (merge new-object {:type :player})))

 (defmethod update-object :color [object]
   {:color 'white :type :color})
 
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

  ;; again, we kinda cheat and allow closures to capture some info we should be passing in
  (defmulti render-object :type)

  ;(defmethod render-text :text [object]
  ;  (.setColor gfx (java.awt.Color/RED))

;    (.fillRect gfx (:x object) (:y object) 20 20))

  (defmethod render-object :player [object]
    (.setColor gfx (java.awt.Color/RED))

    (.fillRect gfx (:x object) (:y object) 20 20))

  (defmethod render-object :default [object]
    ;; do nothing (we dont have to render EVERY part of the state)
    )

  (dorun (map-hash (fn [key value] (render-object value)) state))

  ;; example sprite usage
  ;; (draw-sprite sprites [0 0] gfx [350 350])
)

(defn init []
  { :player {:x 50 :y 50 :color 'red :type :player}
    ;:text {:x 20 :y 20 :content "Fnord"}
    :background-color {:color 'white :type :color}
  })

;; The #' is for playing nice with the REPL. Sends var, not actual obj
(defn x []
  (res-start
    { :init-fn #'init
      :update-fn #'update-state
      :render-fn #'render-game}))

(x)


