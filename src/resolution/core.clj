(ns resolution.core
  (:import
   (java.awt Dimension Toolkit Font)
   (java.io File)
   (java.awt.event KeyListener)

   (javax.imageio ImageIO)
   (javax.swing JPanel JFrame)))

;;; TODO: (by ludum dare)
;;; music
;;; barebones engine
;;; computer-aware sleeping (that is, Thread/sleep sleeps for the right amt of time to
;;; make framerates reasonable).
;;; test packaging as a JAR.


;;; utility

(defn render-text [x y text gfx color]
 "Writes TEXT at location X, Y on graphics GFX in color COLOR."
  (let [font (Font. "Serif" Font/PLAIN 14)]
    (.setColor gfx color)
    (.setFont font)
    (.drawString gfx text x y)))

;;; keypress functions

(def keys-down (atom #{}))              ;keys currently pressed
(def keys-up (atom #{}))                ;keys recently released

(defn key-up? [key]
"Did the user just release KEY? This is for things like menu selection when you don't want the
user to press UP for like 50 ms and have them watch in horror as the selection moves 5 times
because your sleep rate is only 10 ms."
  (let [was-up? (@keys-up key)]
    (swap! keys-up disj key)
    was-up?))

(defn key-down? [key]
"Is the user holding KEY right now?"
  (if (@keys-down key) 1 0))

(defn all-keys-down []
"Get the set of all pressed keys as keycodes."
  @keys-down)

;; spritesheets

(defn load-spritesheet [path tile-size]
  "Given PATH, a path to a spritesheet, and TILE-SIZE, the width and height of each tile (yes, they
are require to be square for now), returns an array of tiles."
  (let [sheet (ImageIO/read (File. path))
        width (.getWidth sheet)
        height (.getHeight sheet)
        tile-width (/ width tile-size)
        tile-height (/ height tile-size)
        tiles (doall (for [x (range tile-width)]
                       (for [y (range tile-height)]
                           (.getSubimage sheet (* x tile-size) (* y tile-size) tile-size tile-size))))
        tile-vec (vec (map vec tiles)) ;2d list-> 2d vec
        ] 
    tile-vec))

(defn draw-sprite [src-sprites src-pos dst-gfx dst-pos]
  (let [[dest-x dest-y] dst-pos]
    (.drawImage dst-gfx (get-in src-sprites src-pos)
                nil dest-x dest-y ))
  )

;;; double-buffering

(defn double-buffer [render-fn & args]
  "Given a render-fn that renders graphics, double buffers it.
It will be called like so: (render-fn gfx-object & args)."
  (fn []
    (let [bfs (.getBufferStrategy frame)
          gfx (.getDrawGraphics bfs)]
      (apply render-fn (cons gfx args))
      ;; double buffer
      (.show bfs)
      (.sync (Toolkit/getDefaultToolkit)))))


;;; core

(defn start [size game-loop]
  "Start a new game. Opens up a window with dimensions size * size,
   and runs game-loop in a separate thread."
  (def panel
    (proxy [JPanel KeyListener] []
      (getPreferredSize [] (Dimension. size size))
      
      (keyPressed [e]
        (let [key-code (.getKeyCode e)]
          (swap! keys-down conj key-code)))
         
      (keyReleased [e]
        (let [key-code (.getKeyCode e)]
          (swap! keys-down disj key-code)
          (swap! keys-up conj key-code)
       ))

      (keyTyped [e])))

  (doto panel
    (.setFocusable true)
    (.addKeyListener panel))

  (def frame (JFrame. "Test"))

  (doto frame
      (.add panel)
      (.pack)
      ; (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE) ; Argh! So frustrating.
      (.createBufferStrategy 2)
      (.setVisible true))
  
  (game-loop frame {})
  ;; (def f (future-call (bound-fn [] (game-loop frame))))
 ) 

