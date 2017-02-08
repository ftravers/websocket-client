(ns websocket-client.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [websocket-client.core-test]))

(doo-tests 'websocket-client.core-test)

