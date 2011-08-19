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
         dx (+ (if (key-down? 39)  5 0) (if (key-down? 37) -5 0))
         dy (+ (if (key-down? 38) -5 0) (if (key-down? 40)  5 0))
         new-object object
         new-object (let [new-pos (merge-with + new-object {:x dx})]
                      (if (not (touches-wall? new-pos map1)) new-pos new-object))
         new-object (let [new-pos (merge-with + new-object {:y dy})]
                      (if (not (touches-wall? new-pos map1)) new-pos new-object))
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
    :background-color {:color 'white :type :color}
  })

;; The #' is for playing nice with the REPL. Sends var, not actual obj
(res-start
  { :init-fn #'init
    :update-fn #'update-state
    :render-fn #'render-game})

