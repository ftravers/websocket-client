<div id="table-of-contents">
<h2>Table of Contents</h2>
<div id="text-table-of-contents">
<ul>
<li><a href="#sec-1">1. WebSocket Client</a></li>
<li><a href="#sec-2">2. Clojars</a></li>
<li><a href="#sec-3">3. Usage</a></li>
</ul>
</div>
</div>

# WebSocket Client<a id="sec-1" name="sec-1"></a>

This is the cljs (client) side of the websocket connection.  This
should be paired with [ftravers/websocket-server](https://github.com/ftravers/websocket-server).

# Clojars<a id="sec-2" name="sec-2"></a>

![](https://clojars.org/fentontravers/websocket-client/latest-version.svg)
  
# Usage<a id="sec-3" name="sec-3"></a>

```clj  
(ns ...
  (:require [cljs.core.async :refer [<! >! chan]]
            [websocket-client.core :refer [init-websocket!]]))
(defn websocket-test []
  (let [send-chan (chan)
        recv-chan (chan)]
    (init-websocket! send-chan recv-chan "ws://localhost:7890")
    (go (>! send-chan "A test message.")
        (.log js/console (<! recv-chan)))))
```
  
We always send strings over websockets.  One interesting usecase (but
potentially dangerous) is to send EDN over the websocket, for this
case we just convert the EDN to and from a string.  

```clj
(ns ... 
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :refer [<! >! chan]]
            [websocket-client.core :refer [init-websocket!]]))

(defn websocket-edn-test []
  (let [send-chan (chan)
        recv-chan (chan)]
    (init-websocket! send-chan recv-chan "ws://localhost:7890")
    (go (>! send-chan (str {:count 1}))
        (is (= {:count 11} (read-string (<! recv-chan)))))))
```
  
This corresponds to a request handler on the server that looks like:

```clj  
(defn request-handler-edn-add10
  "This function will take some EDN and increment a value by 10, and send it back."
  [data]
  (println "Received Data: " (str data))
  (let [req (edn/read-string data)
        resp (str {:count (+ 10 (:count req))})]
    (println "Sending Resp: " resp)
    resp))
```