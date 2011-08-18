(ns resolution.core
  (:import
   (java.awt Dimension Toolkit Font)
   (java.io File)
   (java.awt.event WindowAdapter KeyListener)

   (javax.imageio ImageIO)
   (javax.swing JPanel JFrame)))

(def running (atom true))

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

(defn double-buffer [render-fn frame & args]
  "Given a render-fn that renders graphics, double buffers it.
It will be called like so: (render-fn gfx-object & args). You
should render all graphics to gfx."
  (let [bfs (.getBufferStrategy frame)
        gfx (.getDrawGraphics bfs)]
    (apply render-fn (cons gfx args))
    ;; double buffer
    (.show bfs)
    (.sync (Toolkit/getDefaultToolkit))))


;;; core

(defn make-frame [size]
  "Creates the frame that the game will run in. Also hooks up
key events."
  (let [panel
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

            (keyTyped [e]))]

    (.setFocusable panel true)
    (.addKeyListener panel panel)

    (let [frame (JFrame. "Test")]
      (.add frame panel)
      (.pack frame)
      (.setVisible frame true)
      (.createBufferStrategy frame 2)
      (.setDefaultCloseOperation frame JFrame/DO_NOTHING_ON_CLOSE)
      (.addWindowListener frame
       (proxy [WindowAdapter] []
         (windowClosing [e]
           (reset! running false)
           (.dispose frame))))

      frame))
 ) 

(defn res-start [functions]
  "Main function of Resolution. Takes keyword arguments:
  (Well, it doesn't now, but it will...)
  :init-fn
     Function that returns the initial game state. Takes [].
  :update-fn:
     Function to update the game state. Takes old game state.
  :render-fn
     Renders the game. Takes gfx, state."
  (reset! running true)
  (let [frame (make-frame 500)
        {init-fn :init-fn
         update-fn :update-fn
         render-fn :render-fn} functions]

    (let [initial-state (init-fn)]
      (loop [state initial-state]
        (double-buffer render-fn frame initial-state)
        (Thread/sleep 10)
        (if @running
          (recur (update-fn state)))))))
