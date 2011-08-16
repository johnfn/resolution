(ns sample.core
  (:use resolution.core))
 
;;;; This is a sample project intended to show how to use Resolution.
 
;; initialization and update of state have really similar parts. I wonder if they can
;; be joined together somehow.
 
;; TODO: type is a redundancy here. (with res-init)
(def init
  { :player {:x 5 :y 5 :color 'red :type :player}
    :background-color {:color 'white :type :color}
  })
 
(defn map-hash [update-fn hash]
  "replace every key, value pair of HASH with key, (fn key value)"
  (into {} (map (fn [key-value] (assoc key-value 1 (apply update-fn key-value))) (into [] hash))))
 
(defn res-update [old-state]
  ;;these multimethods must be defined inside res-update in order to gain
  ;; closures over state etc. 

  ;; I think this is bad style, should probably be passing those in.
   
            
 (defmulti update-object :type)
 
 (defmethod update-object :player [object]
   {:x 4 :y 4 :type :player})
 (defmethod update-object :color [object]
   {:color 'white :type :color})
 
 (defn new-objects [old-state]
   [:game-over true])
  
  ; map can update extant items and remove them, but not add.
  ; for some odd reason i cannot think of a better way to solve this.
  


 (conj
   (map-hash (fn [key value] (update-object value)) old-state)
   (new-objects old-state))
   )

 
(defn end-game[]
  (println "GAME OVER."))

(defn render-game[]
  (println "Render loop"))
 
(defn res-loop [initial-state]
 (let [screen 1]
   (loop [state initial-state]
     (render-game)
     (if (:game-over state)
       (end-game)
       (recur (res-update state))))))
 
(defn -main [initial-res-state]
  (let [state initial-res-state]
    (res-loop initial-res-state))
  )
 
(-main init)

