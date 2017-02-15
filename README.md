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

![img](//clojars.org/fentontravers/websocket-client/latest-version.svg)

literal html below

<div class="export">
<img src="<https://camo.githubusercontent.com/ccd2234bd230e37b073f327b5b4f7112d4f73fd6/68747470733a2f2f636c6f6a6172732e6f72672f66656e746f6e747261766572732f776562736f636b65742d636c69656e742f6c61746573742d76657273696f6e2e737667>" alt="" data-canonical-src="![img](//clojars.org/fentontravers/websocket-client/latest-version.svg)" style="max-width:100%;">

</div>

# Usage<a id="sec-3" name="sec-3"></a>

    (ns ...
      (:require [cljs.core.async :refer [<! >! chan]]
                [websocket-client.core :refer [init-websocket!]]))
    
    (defn websocket-test []
      (let [url  "ws://localhost:7890"
            aws (async-websocket url)  ;; initialize the websocket
            ]
    
        ;; Write into the websocket
        (go (>! aws "Sending a test messsage."))
    
        ;; Read out of the websocket
        (go (.log js/console (<! aws)))))

We always send strings over websockets.  One interesting usecase (but
potentially dangerous) is to send EDN over the websocket, for this
case we just convert the EDN to and from a string.  

    (ns ... 
      (:require [cljs.reader :refer [read-string]]
                [cljs.core.async :refer [<! >! chan]]
                [websocket-client.core :refer [init-websocket!]]))
    
    (defn websocket-edn-test []
      (let [send-chan (chan)
            recv-chan (chan)]
        (init-websocket! send-chan recv-chan "ws://localhost:7890")
        (go (>! send-chan (str {:count 1}))
            (.log js/console (str (= {:count 11} (read-string (<! recv-chan))))))))

This corresponds to a request handler on the server that looks like:

    (defn request-handler-edn-add10
      "This function will take some EDN and increment a value by 10, and send it back."
      [data]
      (println "Received Data: " (str data))
      (let [req (edn/read-string data)
            resp (str {:count (+ 10 (:count req))})]
        (println "Sending Resp: " resp)
        resp))