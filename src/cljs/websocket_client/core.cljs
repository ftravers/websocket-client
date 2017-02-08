(ns websocket-client.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :refer [<! >! chan timeout]]))

;; We break up the send channel into two, one specifically for the
;; application we call this the app-send-chan.  This channel is
;; persistent and the application can rely on it always being there
;; and the same.  On the otherhand the ws-only-send-chan goes away
;; often (each time the socket closes, we discard it, and the
;; associated ws-only-send-chan) and create a brand-new websocket.
;; This is why we have to break up the send channel, so the app can
;; have a reliably there channel to work with.

(enable-console-print!)

(defn log [& args] (.log js/console (reduce str args)))

(defonce coms (atom {:ws-url ""
                     :websocket nil
                     :ws-only-send-chan nil
                     :app-send-chan nil
                     :app-recv-chan nil}))

(def ws-states {0 :not-connected-yet
                1 :ready-to-transmit
                3 :ws-closed})

(defn print-coms! []
  (log "Websocket State: >>" (ws-states (.-readyState (:websocket @coms))) "<<"))

(defn on-open []
  (go
    (loop []
      (let [msg (<! (:ws-only-send-chan @coms))]
        (log "Popped message, off websocket only send channel: >>" msg "<<")
        (log "Will now send over websocket.")
        (print-coms!)
        (.send (:websocket @coms) msg)
        (log "Sent message over websocket.  Now will wait for next message to send."))
      (recur))))

(defn make-websocket! []
  "Make new websocket, wire up send/recv channels."
  (log "Using websocket url: >>" (:ws-url @coms) "<<")
  (swap! coms assoc-in [:websocket] (js/WebSocket. (:ws-url @coms)))
  (swap! coms assoc-in [:ws-only-send-chan] (chan))
  (aset (:websocket @coms) "onopen" on-open)
  (aset (:websocket @coms)
        "onmessage"
        #(go
           (let [resp (aget % "data")]
             (log "Got message from websocket: >>" resp "<<")
             (log "Putting message on app receive channel.")
             (>! (:app-recv-chan @coms) resp))))
  (log "Set websocket and core.async send/recv channels: >>" @coms "<<"))

(defn- ensure-websocket-connected! [attempt]
  "If websocket is not in ready state, discard and make new one."
  (log "Checking that websocket is in correct state.  Attempt >>" attempt "<<")
  (if (> attempt 2)
    (throw (js/Error. "Unable to connect to websocket.  Timed out."))
    (let [state (ws-states (.-readyState (:websocket @coms)))]
      (case state
        :ws-closed (do (log "Websocket closed, make a new one.")
                       (make-websocket!))
        :not-connected-yet (do (log "Websocket not connected yet, waiting 1/3 of a second")
                               (go (<! (timeout 300))
                                   (log "Waited 1/3 of a second.")
                                   (log "Next attempt: >>" (inc attempt) "<<")
                                   (ensure-websocket-connected! (inc attempt))))
        :ready-to-transmit (log "Ready to transmit! Sweet!")
        (throw (js/Error. "Unable to connect to websocket.  Reason unknown."))))))

(defn monitor-send-msg-queue!
  [app-send-chan]
  "We need to check if websocket has timed out before we try to send
  on it.  If it has timed out, we create a new websocket and send on
  that."
  (go
    (loop []
      (log "Checking to see websocket is connected.")
      (ensure-websocket-connected! 0)
      (log "Websocket is connected!  Wait for message on app send channel.")
      (let [msg (<! app-send-chan)
            ws (:websocket @coms)
            ws-only-send-chan (:ws-only-send-chan @coms)]
        (log "Got message on application send channel: >>" msg "<<")
        (log "Move onto websocket only send channel.")
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

;; (defn- test-ws! []
;;   (let [send-chan (chan)
;;         recv-chan (chan)
;;         ws-url "ws://localhost:7890"]
;;     (set-parms! send-chan recv-chan ws-url))
;;   (log (str (:ws-url @coms)))
;;   (make-websocket!)
;;   (go (monitor-send-msg-queue! (:app-send-chan @coms)))
;;   (go (>! (:app-send-chan @coms) "Dummy Test.")
;;       (log "From app received channel got message:"
;;               (str (<! (:app-recv-chan @coms))))))

