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

(defn get-recent-scores-graphql-json
  [game-key]
  (clj->json {:query (str "query recentScores($game_key: String!)"
                          "{ recentScores(game_key:$game_key) "
                          "{ score id created name}}")
              :variables {:game_key game-key}}))

(defn get-recent-scores
  [game-key response-fn]
  (xhr/retrieve-url leaderboard-url
                    "POST"
                    (get-recent-scores-graphql-json game-key)
                    (xhr/process-json-response response-fn)))

(defn get-top-scores-graphql-json
  [game-key keyword]
  (clj->json {:query (str "query topScores($game_key: String!,$keyword: String!,)"
                          "{ topScores(game_key:$game_key,keyword: $keyword) "
                          "{ score id created name }}")
              :variables {:game_key game-key
                          :keyword keyword}}))

(defn get-top-scores
  [game-key keyword response-fn]
  (xhr/retrieve-url leaderboard-url
                    "POST"
                    (get-top-scores-graphql-json game-key keyword)
                    (xhr/process-json-response response-fn)))
