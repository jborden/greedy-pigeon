(ns greedy-pigeon.components
  (:require-macros [reagent.interop :refer [$ $!]])
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [greedy-pigeon.display :as display]
            [greedy-pigeon.leaderboard :as leaderboard]
            [greedy-pigeon.utilities :as utilities]))

(defn ProcessingIcon
  []
  (fn []
    [:i {:class "fa fa-lg fa-spinner fa-pulse "
         :style {:color "black"}}]))

(defn TitleScreen
  []
  (fn [{:keys [selected-menu-item]}]
    [:div {:id "title-screen"
           :style {:position "absolute"}}
     [:img {:src "images/title.png"
            :style {:margin "0 auto"
                    :height "inherit"
                    :display "block"}}]]))

(defn GameWonScreen
  []
  (fn [{:keys [selected-menu-item]}]
    [:div {:id "title-screen"}
     [:div {:id "title"
            :style {:color "#FFF"}}
      "You Win!"]
     [:div {:id "title-menu"}
      [:div {:id "start"}
       [:div {:class "selection-symbol"}
        (str (if (= @selected-menu-item
                    "play-again")
               "→"
               ""))]
       " Play Again"]
      [:div {:id "foo"}
       [:div {:class "selection-symbol"}
        (str (if (= @selected-menu-item
                    "title-screen")
               "→"
               ""))]
       " Title Screen"]]]))

(defn GameLostScreen
  []
  (fn [{:keys [url score stage]}]
    [:div {:id "title-screen"
           :style {:position "absolute"}}
     [:img {:src url
            :style {:margin "0 auto"
                    :height "inherit"
                    :display "block"}}]
     [:div {:style {:color "white"
                    :position "absolute"
                    :top "0"
                    :margin "0 auto"
                    :width "100%"
                    :text-align "center"
                    :font-size "1.5em"}}
      (str "You got to Stage " stage " with a Score of " score)]

     ]))

(defn PauseComponent
  "Props is:
  {:paused? ; r/atom boolean
  :on-click ; fn
  }
  "
  [props]
  (fn [props]
    (let [{:keys [paused? on-click]} props]
      [:div {:id "blocker"
             :style (if @paused?
                      {}
                      {:display "none"})}
       [:div {:id "instructions"
              :style (if @paused?
                       {}
                       {:display "none"})
              :on-click on-click}
        [:span {:style {:font-size "40px"}}
         "Click or Press Space Key to Unpause  "]
        [:br]
        "W, A, S, D / Arrow Keys = Move, "]])))

(defn AssetLoadingComponent
  "Props is:
   {:assets-loaded-percent ; r/atom number}"
  [props]
  (fn [props]
    (let [{:keys [assets-loaded-percent]} props]
      [:div {:style {:display (str (if (= @assets-loaded-percent 1)
                                     "none"
                                     "block"))
                     :position "absolute"
                     :width "100%"
                     :height "100%"}}
       [:div {:id "instructions"}
        [:div {:style {:width "100%"
                       :position "fixed"
                       :bottom "1em"}}
         [:div {:style {:background-color "darkgray"
                        :height "20px"
                        :width "10em"
                        :margin "0 auto"}}
          [:div {:style {:background-color "white"
                         :width (str (* @assets-loaded-percent 100) "%")
                         :height "20px"}}]]]]])))

(defn GameContainer
  [{:keys [renderer camera state]}]
  (let [on-blur #(swap! state assoc :paused? true)
        on-resize #(display/window-resize! renderer camera)]
    (r/create-class
     {:display-name "game-container"

      :component-did-mount
      (fn [this]
        (dom/appendChild (r/dom-node this) ($ renderer :domElement))
        ($ js/window addEventListener "blur" on-blur)
        ($ js/window addEventListener "resize" on-resize false))

      :component-will-unmount
      (fn [this]
        ($ renderer forceContextLoss)
        ($ js/window removeEventListener "blur" on-blur)
        ($ js/window removeEventListener "resize" on-resize))

      :reagent-render (fn []
                        [:div {:id "game-container"
                               :style {:position "absolute"
                                       :left "0px"
                                       :top "0px"}}])})))

(defn TextInput
  "props is:
  {
  :value          ; str
  :default-value  ; str
  :placeholder    ; str, optional
  :on-change      ; fn, fn to execute on change
  }
  "
  [props]
  (fn [{:keys [value default-value placeholder on-change]
        :or {default-value ""}} props]
    [:input {:type "text"
             :class "form-control-purple"
             :value value
             :defaultValue default-value
             :placeholder placeholder
             :on-change on-change}]))

(defn MenuButton
  [props text]
  (fn [{:keys [on-click id]} text]
    [:a {:href "#"
         :id id
         :on-click  on-click
         :class "menu-button"} text]))

(defn GameOverMenu
  [props form]
  (fn [props form]
    [:div {:id "menu"
           :style {:position "absolute"
                   :z-index "3"
                   :left "35%"
                   :top "15%"
                   :color "red"
                   :background-color "lightgrey"}}
     [:div {:id "module-padding"
            :style {:padding "0.5em"
                    :overflow "hidden"}}
      @form]]))

(defn ScoreForm
  [props]
  (fn [{:keys [score stage restart-fn add-to-leaderboard-fn]}]
    [:form {:id "username-form"
            :method "post"}
     [:h1 "Your Score"]
     [:h1 (str "Points " score)]
     [:div
      [MenuButton {:on-click restart-fn } "Play Again"]
      [MenuButton {:on-click add-to-leaderboard-fn} "Add to Leaderboard"]]]))

(defn InputNameForm
  [props]
  (fn [{:keys [score stage name-on-change submit-fn restart-fn
               game-name retrieving?]}]
    [:form {:id "username-form"
            :method "post"}
     [:h1 "Submit Score"]
     [:h1 (str "Points " score)]
     [:div {:id "game-name-containers"}
      [TextInput {:value @game-name
                  :default-value "Foo"
                  :placeholder "Game Name"
                  :on-change name-on-change}]]
     (if @retrieving?
       [:div {:class "menu-button"} [ProcessingIcon]]
       [MenuButton {:on-click (partial submit-fn score stage @game-name)}
        "Submit"])
     [MenuButton {:on-click restart-fn} "Play Again"]]))

(defn LeaderboardTableRow
  [row]
  (fn [row]
    (let [{:keys [name score id]} row
          {:keys [score stage]} (utilities/json->clj score)]
      [:tr
       (doall (map identity
                   (mapv (fn [item]
                           ^{:key (gensym "key")}
                           [:td item])
                         [name score stage])))])))

(defn LeaderboardTableHeader
  []
  (fn []
    [:thead
     [:tr [:td "Name"] [:td "Score"] [:td "Stage"]]]))

(defn LeaderboardForm
  [props]
  (fn [{:keys [game-name submit-fn]}]
    (let [score-keyword "score"
          latest-entries (r/atom [])
          top-entries (r/atom [])]
      (r/create-class
       {:display-name "leaderboard-scores"

        :component-did-mount
        (fn [this]
          (leaderboard/get-recent-scores (fn [response]
                                           (reset! latest-entries
                                                   (get-in response [:data :recentScores]))))
          (leaderboard/get-top-scores score-keyword
                                      (fn [response]
                                        (reset! top-entries (get-in response [:data :topScores])))))

        :reagent-render (fn []
                          [:form
                           [:h1 "Latest Entries"]
                           [:table
                            [:tbody
                             [LeaderboardTableHeader]
                             (map (fn [entry]
                                    ^{:key (:id entry)}
                                    [LeaderboardTableRow entry])
                                  @latest-entries)]]
                           [:h1 "Top Entries"]
                           [:table
                            [:tbody
                             [LeaderboardTableHeader]
                             (map (fn [entry]
                                    ^{:key (:id entry)}
                                    [LeaderboardTableRow entry])
                                  @top-entries)]]
                           [MenuButton {:on-click submit-fn} "Back to Game"]])}))))
