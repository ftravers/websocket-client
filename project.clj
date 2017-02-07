(defproject websocket-client "0.2.1"
  :description "WebSocket Client Library"
  :url "https://github.com/ftravers/websocket-client"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.456"]
                 [org.clojure/core.async "0.2.395" :exclusions [org.clojure/tools.reader]]]
  
  :source-paths ["src/cljs"]
  :clean-targets ^{:protect false} ["target" "resources/public/js"]
  :target-path "target/%s"
  :plugins [[lein-figwheel "0.5.4-7"]
            [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]
            [cider/cider-nrepl "0.15.0-SNAPSHOT"]
            [lein-pprint "1.1.2"]]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs" ]
                :figwheel true
                :compiler {:main websocket-client.core
                           :asset-path "js"
                           :output-to "resources/public/js/main.js"
                           :output-dir "resources/public/js"
                           :verbose true
                           :source-map-timestamp true}}]}

  :repositories [["clojars" {:url "https://clojars.org/repo"
                             :creds :gpg}]]
  
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [figwheel-sidecar "0.5.4-7"]                                   
                                  [com.cemerick/piggieback "0.2.1"]]
                   :source-paths ["src/cljs"]
                   :repl-options {:init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
