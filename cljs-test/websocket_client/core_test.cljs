(ns websocket-client.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [use-fixtures async deftest is testing run-tests]]
            [cljs.core.async :refer [<! >! chan]]
            [cljs.reader :refer [read-string]]
            [websocket-client.core :refer [init-websocket!]]))

;; (deftest websocket-test
;;   (let [ws-url "ws://localhost:7890"
;;         send-chan (chan)
;;         recv-chan (chan)
;;         send-msg (clojure.string/lower-case "A message from tester.")]
;;     (init-websocket! send-chan recv-chan ws-url)
;;     (async done
;;            (go (>! send-chan send-msg)
;;                (is (= send-msg (clojure.string/lower-case (<! recv-chan))))
;;                (done)))))

(deftest websocket-edn-test
  (let [send-chan (chan)
       recv-chan (chan)]
       (init-websocket! send-chan recv-chan "ws://localhost:7890")
       (async done
              (go (>! send-chan (str {:count 1}))
                  (is (= {:count 11} (read-string (<! recv-chan))))
                  (done)))))

(defn websocket-edn-test []
  (let [send-chan (chan)
       recv-chan (chan)]
       (init-websocket! send-chan recv-chan "ws://localhost:7890")
       (async done
              (go (>! send-chan (str {:count 1}))
                  (is (= {:count 11} (read-string (<! recv-chan))))
                  (done)))))

(run-tests)
