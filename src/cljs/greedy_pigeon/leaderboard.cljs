(ns greedy-pigeon.leaderboard
  (:require [greedy-pigeon.xhr :as xhr]
            [greedy-pigeon.utilities :refer [clj->json leaderboard-url]]))

(defn post-score-graphql-json
  [score-map game-key name]
  (clj->json {:query (str "mutation M($score: String!, $game_key: String!,$name: String!)"
                          "{ score(game_key: $game_key, name: $name,score: $score)"
                          " { id }}")
              :variables {:score (clj->json score-map)
                          :game_key game-key
                          :name name}}))

(defn post-score
  [score-map game-key name response-fn]
  (xhr/retrieve-url leaderboard-url
                    "POST"
                    (post-score-graphql-json score-map game-key name)
                    (xhr/process-json-response response-fn)))
