(ns resolution.core
  (:import
   (java.awt Dimension)
   (java.awt.event KeyListener)
   (javax.swing JPanel JFrame)))

;;; TODO: (by ludum dare)
;;; spritesheet abstraction
;;; music
;;; computer-aware sleeping (that is, Thread/sleep sleeps for the right amt of time to
;;; make framerates reasonable).




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
  
  (def f (future-call (bound-fn [] (game-loop frame))))
 ) 

