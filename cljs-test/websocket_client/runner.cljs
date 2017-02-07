(ns cljs-websocket-coreasync-simple.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [cljs-websocket-coreasync-simple.core-test]))

(doo-tests 'cljs-websocket-coreasync-simple.core-test)

