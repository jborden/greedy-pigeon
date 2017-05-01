(ns greedy-pigeon.dev
  (:require-macros [reagent.interop :refer [$]])
  (:require [greedy-pigeon.core :as core]
            [reagent.core :as r]))

(defn ^:export on-jsload
  []
  (core/load-game-assets)
  (r/unmount-component-at-node ($ js/document getElementById "reagent-app"))
  (core/init-game-container core/state))
