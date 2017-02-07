(ns cljs-websocket-coreasync-simple.core-test
    (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [use-fixtures async deftest is testing run-tests]]
            [cljs.core.async :refer [<! >! chan]]
            [cljs-websocket-coreasync-simple.core :refer [init-websocket! set-parms! make-websocket! monitor-send-msg-queue! print-coms!]]))

(use-fixtures :once 
  {:before (fn [] (println "before"))
   :after  (fn [] (println "after"))})

(defn- dbug [& msg] (println (str msg)))

(deftest init-websocket!-test
  (let [ws-url "ws://localhost:8062"
        send-chan (chan)
        recv-chan (chan)
        send-msg "A message from tester."]
    (set-parms! send-chan recv-chan ws-url)
    (make-websocket!)
    (monitor-send-msg-queue! send-chan)
    (print-coms!)    
    (async done
           (go (>! send-chan send-msg)
               (is (= send-msg (<! recv-chan)))
               (done)))))

;; (def coms (atom {}))
;; (defn tester []
;;   (let [send-chan (chan)
;;         recv-chan (chan)
;;         ws-url "ws://localhost:8062"]
;;     (swap! coms assoc-in [:send-chan] send-chan)
;;     (swap! coms assoc-in [:recv-chan] recv-chan)
;;     (swap! coms assoc-in [:ws-url] ws-url)))

;; (defn t2 []
;;   (let [send-chan (chan)
;;         recv-chan (chan)
;;         ws-url "ws://localhost:8062"]
;;     (set-parms! send-chan recv-chan ws-url)))

(run-tests)
