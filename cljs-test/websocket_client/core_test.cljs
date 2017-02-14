(ns websocket-client.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [use-fixtures async deftest is testing run-tests]]
            [cljs.core.async :refer [<! >! chan]]
            [cljs.reader :refer [read-string]]
            [websocket-client.core :refer [async-websocket]]))

(def url "ws://localhost:7890")

;; (deftest websocket-test
;;   (let [ws-url "ws://localhost:7890"
;;         aws (async-websocket ws-url)
;;         send-msg (clojure.string/lower-case "A message from tester.")]
;;     (async
;;      done
;;      (go (>! aws send-msg)
;;          (is (= send-msg (clojure.string/lower-case (<! aws))))
;;          (done)))))

;; (deftest websocket-edn-test
;;   (let [send-chan (chan)
;;        recv-chan (chan)]
;;        (init-websocket! send-chan recv-chan "ws://localhost:7890")
;;        (async done
;;               (go (>! send-chan (str {:count 1}))
;;                   (is (= {:count 11} (read-string (<! recv-chan))))
;;                   (done)))))

;; (run-tests)
