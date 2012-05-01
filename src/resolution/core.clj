(ns resolution.core
  (:import
   (java.awt Dimension Toolkit Font)
   (java.io File)
   (java.awt.event WindowAdapter KeyListener)

   (javax.imageio ImageIO)
   (javax.swing JPanel JFrame)))

(def running (atom true))


;;; clj-utility

(def printed (atom #{}))

(defn future-bind-out [fn]
  "Like (future fn), except that we bind *out* correctly so that
stuff like (println) still works."
  (def f (future-call (bound-fn [] (fn)))))

(defn pro [& args]
  "Print out, but rate limit to once every 10 seconds."
  (future-bind-out
   (fn [] (if (not (@printed args))
            (do
              (swap! printed conj args)
              (apply println args)
              (Thread/sleep 10000)
              (swap! printed disj args)
              )))))

;;; utility

(defn map-hash [update-fn hash]
  "replace every key, value pair of HASH with key, (fn key value)"
  (into {} (map (fn [key-value] (assoc key-value 1 (apply update-fn key-value))) (into [] hash))))

(defn strs-to-vec [& args]
  "convert args into 2D array (or 1D as a special case)."
  (vec (map vec args)))

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
  (@keys-down key))

(defn all-keys-down []
"Get the set of all pressed keys as keycodes."
  @keys-down)

;; spritesheets

(defn color-key [img color]
  (doseq [i (range (.getHeight img)) j (range (.getHeight img))]
    (when (== (.getRGB img i j) (.getRGB color))
      (.setRGB img i j (.getRGB (new java.awt.Color 0 255 255 255)))))
  img)

(defn load-spritesheet [path tile-size]
  "Given PATH, a path to a spritesheet, and TILE-SIZE, the width and height of each tile (yes, they
are require to be square for now), returns an array of tiles."
  (let [sheet (color-key (ImageIO/read (File. path)) java.awt.Color/WHITE)
        width (.getWidth sheet)
        height (.getHeight sheet)
        tile-width (/ width tile-size)
        tile-height (/ height tile-size)
        tiles (doall (for [x (range tile-width)] (for [y (range tile-height)]
                                   (.getSubimage sheet (* x tile-size) (* y tile-size) tile-size tile-size))))
        tile-vec (vec (map vec tiles)) ;2d list-> 2d vec
        ] 
    tile-vec))

(defn draw-sprite [src-sprites src-pos dst-gfx dst-pos]
  (let [[dest-x dest-y] dst-pos]
    (.drawImage dst-gfx (get-in src-sprites src-pos)
                nil dest-x dest-y ))
  )

;;; graphics

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
         check-fn :check-fn
         update-fn :update-fn
         render-fn :render-fn} functions]

    (let [initial-state (init-fn)]
      (loop [state initial-state] ; check for changes in the initial state.
        (let [state (merge state (check-fn))]
          (double-buffer render-fn frame state)
          (do
            (Thread/sleep 5)
            (if @running
              (recur (update-fn state)))))))))
