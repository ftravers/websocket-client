
# WebSocket Client

This is the cljs (client) side of the websocket connection.  This
should be paired with [ftravers/websocket-server](https://github.com/ftravers/websocket-server).

# Clojars


![](https://clojars.org/fentontravers/websocket-client/latest-version.svg)


# Usage

```clojure
(ns ...
  (:require [cljs.core.async :refer [<! >! chan]]
            [websocket-client.core :refer [async-websocket]]))

(defn websocket-test []
  (let [url  "ws://localhost:7890"
        aws (async-websocket url)  ;; initialize the websocket
        ]

    ;; Write into the websocket
    (go (>! aws "Sending a test messsage."))

    ;; Read out of the websocket
    (go (.log js/console (<! aws)))))
```clojure

We always send strings over websockets.  One interesting usecase (but
potentially dangerous) is to send EDN over the websocket, for this
case we just convert the EDN to and from a string.  

```clojure
(defn websocket-test []
  (let [url  "ws://localhost:7890"
        aws (async-websocket url) ;; initialize the websocket
        ]

    ;; write some EDN
    (go (>! aws (str {:count 1})))

    ;; read some EDN
    (go
      (.log
       js/console
       (str (= {:count 11} (read-string (<! aws))))))))
```

This would correspond to a request handler on the server that looks
like:

```clojure
(defn request-handler-edn-add10
  "This function will take some EDN and increment a value by 10, and send it back."
  [data]
  (println "Received Data: " (str data))
  (let [req (edn/read-string data)
        resp (str {:count (+ 10 (:count req))})]
    (println "Sending Resp: " resp)
    resp))
```
