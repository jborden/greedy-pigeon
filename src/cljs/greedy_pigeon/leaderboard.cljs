(ns greedy-pigeon.leaderboard
  (:require [greedy-pigeon.xhr :as xhr]
            [greedy-pigeon.utilities :refer [clj->json]]))

(defn post-score-graphql
  [score-map game-key name]
  {:query (str "mutation M($score: String!, $game_key: String!,$name: String!)"
               "{ score(game_key: $game_key, name: $name,score: $score)"
               " { id }}")
   :variables {:score (clj->json score-map)
               :game_key game-key
               :name name}})
