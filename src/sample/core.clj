(ns sample.core
  (:use resolution.core)
  (:import
    (java.awt Toolkit)
    (java.awt Graphics2D)
   ))

(def window-size 500)

(def printed (atom #{}))

(defn future-bind-out [fn]
  "Like (future fn), except that we bind *out* correctly so that
stuff like (println) still works."
  (def f (future-call (bound-fn [] (fn)))))

(defn pro [str out-s]
  "Print out, but rate limit to once every 10 seconds."
  (future-bind-out
   (fn [] (if (not (@printed str))
            (do
              (swap! printed conj str)
              (let [*out* out-s]
                (prn str))
              (Thread/sleep 10000)
              (swap! printed disj str)
              )))))

;;;; This is a sample project intended to show how to use Resolution.
 
;; initialization and update of state have really similar parts. I wonder if they can
;; be joined together somehow.
 
;; TODO: type is a redundancy here. (with res-init)
 
(defn map-hash [update-fn hash]
  "replace every key, value pair of HASH with key, (fn key value)"
  (into {} (map (fn [key-value] (assoc key-value 1 (apply update-fn key-value))) (into [] hash))))
 
(defn update-state [old-state]
  ;;these multimethods must be defined inside res-update in order to gain
  ;; closures over state etc. 

  ;; I think this is bad style, should probably be passing those in.

  
   
 (defmulti update-object :type)
 
 (defmethod update-object :player [object]
   {:x (+ (:x object)
           1) :y 4 :type :player})
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

(defn render-game [gfx state]
  (.setColor gfx (java.awt.Color/BLACK))
  (.fillRect gfx 0 0 250 250)

  ;; (pro "Herp!" *out*)
  (let [player (:player state)
        x (:x player)
        y (:y player)]
    (.setColor gfx (java.awt.Color/RED))
    (.fillRect gfx 0 0 x y))

  (draw-sprite sprites [0 0] gfx [350 350])
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

