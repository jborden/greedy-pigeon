(defproject greedy-pigeon "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.494"]
                 [cljsjs/soundjs "0.6.2-0"]
                 [cljsjs/three "0.0.84-0"]
                 [reagent "0.6.1"]]
  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-figwheel "0.5.9"]]
  :npm {:dependencies [[source-map-support "0.4.14"]]}
  :source-paths ["src" "target/classes"]
  :clean-targets ^{:protect false} ["release" "resources/public/js"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel {:on-jsload "greedy-pigeon.dev/on-jsload"}
                        :compiler {:main greedy-pigeon.core
                                   :output-to "resources/public/js/greedy_pigeon.js"
                                   :output-dir "resources/public/js/out"
                                   :asset-path "js/out"
                                   :foreign-libs [{:file "resources/js/howler.core.min.js"
                                                   :provides ["howler"]}]
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true}}
                       {:id "release"
                        :source-paths ["src"]
                        :compiler {:output-to "resources/public/js/greedy_pigeon.min.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :foreign-libs [{:file "resources/js/howler.core.min.js"
                                                   :provides ["howler"]}]
                                   :externs ["src/js/greedy_pigeon.externs.js"]}}]}
  :target-path "target")
