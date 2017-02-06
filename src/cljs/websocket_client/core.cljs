(ns websocket-client.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :refer [<! >! chan timeout]]))

(defn blah []
  (.log js/console "Blah"))

(defn lg [msg] (.log js/console msg))
(defn debugf [msg var] (lg (str msg)))
(defn debug [msg] (lg (str msg)))

(defonce coms (atom {:ws-url ""
                     :websocket nil
                     :ws-only-send-chan nil
                     :app-send-chan nil
                     :app-recv-chan nil}))

(defn print-coms! []
  (debugf "Websocket: %s" (:websocket @coms))
  (debugf "Websocket State: %s" (.-readyState (:websocket @coms))))

(defn on-open []
  (go
    (loop []
      (let [msg (<! (:ws-only-send-chan @coms))]
        (debugf "Popped message %s , off websocket only send channel.  Will now send over websocket." msg)
        (print-coms!)
        (.send (:websocket @coms) msg)
        (debug "Sent message over websocket.  Now will wait for next message to send."))
      (recur))))

(defn make-websocket! []
  "Make new websocket, wire up send/recv channels."
  (debugf "Using websocket url: %s" (:ws-url @coms))
  (swap! coms assoc-in [:websocket] (js/WebSocket. (:ws-url @coms)))
  (swap! coms assoc-in [:ws-only-send-chan] (chan))

  (aset (:websocket @coms) "onopen" on-open)

  (aset (:websocket @coms)
        "onmessage"
        #(go
           (let [resp (aget % "data")]
             (debugf "Got message: %s from websocket.  Putting message on app receive channel." resp)
             (>! (:app-recv-chan @coms) resp))))
  (debugf "Set coms: %s" @coms))

(defn- ensure-websocket-connected! [attempt]
  "If websocket is not in ready state, discard and make new one."
  (debugf "Checking that websocket is in correct state.  Attempt %d" attempt)
  (if (> attempt 2)
    (throw (js/Error. "Unable to connect to websocket.  Timed out."))
    (case (.-readyState (:websocket @coms))
      3 (do (debug "Websocket closed, make a new one.")
            (make-websocket!))
      0 (do (debugf "Websocket not connected yet, waiting 1/3 of a second.  Attempt: %d" attempt)
            (go (<! (timeout 300))
                (debug "Waited 1/3 of a second.")
                (debugf "Next attempt %d" (inc attempt))
                (ensure-websocket-connected! (inc attempt))))
      1 (debug "Ready to transmit! Sweet!")
      (throw (js/Error. "Unable to connect to websocket.  Reason unknown.")))))

(defn monitor-send-msg-queue!
  [app-send-chan]
  "We need to check if websocket has timed out before we try to send
  on it.  If it has timed out, we create a new websocket and send on
  that."
  (go
    (loop []
      (debug "Checking to see websocket is connected.")
      (ensure-websocket-connected! 0)
      (debug "Websocket is connected!  Wait for message.")
      (let [msg (<! app-send-chan)
            ws (:websocket @coms)
            ws-only-send-chan (:ws-only-send-chan @coms)]
        (>! ws-only-send-chan msg))
      (recur))))

(defn set-parms!
  [app-send-chan app-recv-chan ws-url]
  (swap! coms assoc-in [:app-send-chan] app-send-chan)
  (swap! coms assoc-in [:app-recv-chan] app-recv-chan)
  (swap! coms assoc-in [:ws-url] ws-url))

(defn init-websocket!
  [app-send-chan app-recv-chan ws-url]
  (set-parms! app-send-chan app-recv-chan ws-url)
  (make-websocket!)
  (monitor-send-msg-queue! app-send-chan))

(defn- test-ws! []
  (let [send-chan (chan)
        recv-chan (chan)
        ws-url "ws://localhost:7890"]
    (set-parms! send-chan recv-chan ws-url))
  (debug (str (:ws-url @coms)))
  (make-websocket!)
  (go (monitor-send-msg-queue! (:app-send-chan @coms)))
  (go (>! (:app-send-chan @coms) "Dummy Test.")))
