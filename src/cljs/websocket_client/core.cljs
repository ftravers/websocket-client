(ns websocket-client.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.reader :refer [read-string]]
            [goog.object :refer [set]]
            [cljs.core.async :as async :refer [chan >! <! timeout]]
            [clojure.core.async.impl.protocols :as impl]))

(declare ws-state log new-websocket! monitor-send-msg-queue! dummy-on-msg)

(defrecord AsyncWebsocket
    [ws-url app-send-chan app-recv-chan ws-only-send-chan websocket]

    impl/ReadPort
    (take! [aws fn-handler]
      (impl/take! (:app-recv-chan aws) fn-handler))

    impl/WritePort
    (put! [aws val fn-handler]
      (impl/put! (:app-send-chan aws) val fn-handler)))

(defn async-websocket [ws-url]
  "AsyncWebsocket constructor fn"
  (-> (->AsyncWebsocket ws-url (chan) (chan) (chan) nil)
      new-websocket!
      monitor-send-msg-queue!))

(defn on-open [aws]
  "Callback.  When WS opens."
  (fn []
    (go
      (loop []
        (let [msg (<! (:ws-only-send-chan aws))]
          (.send (:websocket aws) msg))
        (recur)))))

(defn on-message [aws event]
  "Callback.  When WS receives a message."
  (let [recvd-msg (aget event "data")]
    (go
      (>! (:app-recv-chan aws) recvd-msg))))

(defn new-websocket! [aws]
  "Make new websocket, wire up send/recv channels."
  (let [new-aws (assoc aws :websocket (js/WebSocket. (:ws-url aws)))]
    (set (:websocket new-aws) "onopen" (on-open new-aws))
    (set (:websocket new-aws) "onmessage" (partial on-message new-aws))
    new-aws))

(defn monitor-send-msg-queue!
  [aws]
  "We need to check if websocket has timed out before we try to send
  on it.  If it has timed out, we create a new websocket and send on
  that."
  (go
    (loop []
      (let [msg (<! (:app-send-chan aws))]
        (>! (:ws-only-send-chan aws) msg))
      (recur)))
  aws)

;; -------- Helpers ----------

(defn ws-state [aws]
  ({0 :not-connected-yet
    1 :ready-to-transmit
    3 :ws-closed}
   (.-readyState (:websocket aws))))
