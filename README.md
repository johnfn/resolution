# resolution

A Clojure game libary.

## Usage

### Why would you want to write a game in Clojure?

Oh I dunno...

1. Hot code swapping for games is life changing.

2. It's pretty fast.

3. It's not C++.

4. It's a Lisp.

5. Access to all of Java's libraries.

### Is it practical?

I'll find out. But the question you shuold actually be interested in is:

### Is it fun?

Yes!

### What does FP mean for game programming?

1. No mutable state. This means that what we end up doing is have a big object full of state, and every tick we map over every element in the map - calling its corresponding `update` method - and getting back a new one, which we construct the new state from.

2. Messages and the queue. Since threading the entire world state through every function defeats the purpose of functional programming, functions that update the world state (by adding or removing objects) are managed by inserting a message in a queue, which can later be read in a more convenient place.

TODO: More detail here.

### But isn't all that state destruction/creation incredibly slow?

No!

You have to understand that clojure is really smart about when you do things like `(conj {:a 1 :b 2} [:c 3])`. Clojure doesn't create an entirely new object - it uses the old one and extends it. 

### But I use VIM, and Clojure support is lame!

Not so fast! Install emacs, download vimpulse to get your vim keybindings, and then set up for clojure like normal. Stop shuddering and get over the whole 'holy war' cliche. Emacs and vi are both great editors, but the truth is that emacs is better for clojure editing.


## Explanation

Resolution is a library designed to make writing games in clojure easier. 

It is being developed concurrently with a sample game, with the uninspired name Sample. The reason for this is twofold: it shows the correct useage of Resolution, and it directs my development towards the most important features.

## License

Copyright (C) 2011 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
