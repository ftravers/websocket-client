(ns websocket-client.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.reader :refer [read-string]]
            ;; [cljs.core.async :refer [<! >! chan timeout take! put!]]
            [cljs.core.async :as async :refer [chan >! <! timeout]]
            [clojure.core.async.impl.protocols :as impl]))

(defn log [& args] (.log js/console (reduce str args)))

;; We break up the send channel into two, one specifically for the
;; application we call this the app-send-chan.  This channel is
;; persistent and the application can rely on it always being there
;; and the same.  On the otherhand the ws-only-send-chan goes away
;; often (each time the socket closes, we discard it, and the
;; associated ws-only-send-chan) and create a brand-new websocket.
;; This is why we have to break up the send channel, so the app can
;; have a reliably there channel to work with.

(declare ws-state log new-websocket! monitor-send-msg-queue! dummy-on-msg)

(defrecord AsyncWebsocket [ws-url app-send-chan app-recv-chan ws-only-send-chan websocket]
  impl/ReadPort
  (take! [aws fn-handler]
    (.log js/console "TAKE!: >> " aws " <<")
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
        ;; (log "Wait for messages on [ws-only-send-chan].")
        ;; (log "ws-only-send-chan: >> " (:ws-only-send-chan aws) " <<" )
        (let [msg (<! (:ws-only-send-chan aws))]
          ;; (log "Popped message, off websocket only send channel: >> " msg " <<")
          ;; (log "Will now send over websocket.")
          ;; (log "Websocket State: >> " (ws-state aws) " <<")
          (.send (:websocket aws) msg)
          ;; (log "Sent message over websocket.  Now will wait for next message to send.")
          )
        (recur)))))

(defn on-message [aws event]
  "Callback.  When WS receives a message."
  (fn []
    (let [recvd-msg (aget event "data")]
      (go
        (log "Got message from websocket: >> " recvd-msg " <<")
        (log "Putting message on app receive channel.")
        (>! (:app-recv-chan aws) recvd-msg)))))

(defn dummy-on-msg [event]
  "Callback.  When WS receives a message."
  ;; (.log js/console "!!!!!! got a mesage back!!!!!!!!!")
  (log "Got message from websocket: >> " (aget event "data") " << !!!!!!!!!!"))

(defn new-websocket! [aws]
  "Make new websocket, wire up send/recv channels."
  ;; (log "NWS: Using websocket url: >> " (:ws-url aws) " <<")
  (let [new-aws (assoc aws :websocket (js/WebSocket. (:ws-url aws)))]
    (aset (:websocket new-aws) "onopen" (on-open new-aws))
    ;; (aset (:websocket new-aws) "onmessage" (partial on-message new-aws))
    (aset (:websocket new-aws) "onmessage" dummy-on-msg)
    ;; (log "NWS: Finished configuring and instantiating websocket.")
    new-aws))
(defn monitor-send-msg-queue!
  [aws]
  "We need to check if websocket has timed out before we try to send
  on it.  If it has timed out, we create a new websocket and send on
  that."
  (go
    (loop []
      (let [msg (<! (:app-send-chan aws))]
        ;; (log "MSMQ: Got message on application send channel: >> " msg " <<")
        ;; (log "MSMQ: Move onto websocket only send channel.")
        (>! (:ws-only-send-chan aws) msg))
      (recur)))
  aws)

;; --------- REPL Testing ----------
(def url "ws://localhost:7890")

;; main test:

(def ws1 (async-websocket url))

(defn get-on-msg [aws]
  "get the on-message function for a websocket."
  (aget (:websocket aws) "onmessage"))

;; (go (>! ws1 "Hello"))
;; (go (.log js/console "Answer" (<! ws1)))

;; -------- Helpers ----------

(defn ws-state [aws]
  ({0 :not-connected-yet
    1 :ready-to-transmit
    3 :ws-closed}
   (.-readyState (:websocket aws))))

(enable-console-print!)

