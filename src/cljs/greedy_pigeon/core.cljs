(ns greedy-pigeon.core
  (:require-macros [reagent.interop :refer [$ $!]])
  (:require [reagent.core :as r]
            [cljsjs.three]
            [greedy-pigeon.components :refer [PauseComponent TitleScreen GameContainer GameWonScreen GameLostScreen]]
            [greedy-pigeon.controls :as controls]
            [greedy-pigeon.display :as display]
            [greedy-pigeon.menu :as menu]
            [greedy-pigeon.time-loop :as time-loop]
            [greedy-pigeon.utilities :as utilities]))

(def initial-state {:paused? false
                    :key-state {}
                    :selected-menu-item "start"
                    :time-fn (constantly true)
                    :init-game (constantly true)
                    :init-game-won-fn (constantly true)
                    :init-title-screen-fn (constantly true)
                    :font nil
                    :color-cycle [0xFF69B4 0x69FF83 0xF9FF69]
                    :win-color 0xF9FF69
                    :cycle-colors? false
                    :stage [[0 0 0 0 0 0 1 0 0 0 0 0 0]
                            [0 0 0 0 0 1 0 1 0 0 0 0 0]
                            [0 0 0 0 1 0 1 0 1 0 0 0 0]
                            [0 0 0 1 0 1 0 1 0 1 0 0 0]
                            [0 0 1 0 1 0 1 0 1 0 1 0 0]
                            [0 1 0 1 0 1 0 1 0 1 0 1 0]
                            [1 0 1 0 1 0 1 0 1 0 1 0 1]
                            []]})

(defonce state (r/atom initial-state))

(defn enemy
  []
  (let [geometry (js/THREE.PlaneGeometry. 60 60 1)
        material (js/THREE.MeshBasicMaterial. (clj->js {:color 0xFF0000}))
        mesh (js/THREE.Mesh. geometry material)
        object3d ($ (js/THREE.Object3D.) add mesh)
        box-helper (js/THREE.BoxHelper. object3d 0x00ff00)
        bounding-box (js/THREE.Box3.)
        move-increment 5
        _ ($! object3d :position.z 10)]
    (reify
      Object
      (updateBox [this]
        ($ box-helper update object3d)
        ($ bounding-box setFromObject box-helper))
      (moveLeft [this]
        ($ object3d translateX (- move-increment))
        (.updateBox this))
      (moveRight [this]
        ($ object3d translateX move-increment)
        (.updateBox this))
      (moveUp [this]
        ($ object3d translateY move-increment)
        (.updateBox this))
      (moveDown [this]
        ($ object3d translateY (- move-increment))
        (.updateBox this))
      (moveTo [this x y]
        (do
          ($! object3d :position.x x)
          ($! object3d :position.y y)
          (.updateBox this)))
      (getObject3d [this] object3d)
      (getBoundingBox [this] bounding-box)
      (getBoxHelper [this] box-helper))))

(defn hero
  []
  (let [geometry (js/THREE.PlaneGeometry. 60 60 1)
        material (js/THREE.MeshBasicMaterial. (clj->js {:color 0x0000FF}))
        mesh (js/THREE.Mesh. geometry material)
        object3d ($ (js/THREE.Object3D.) add mesh)
        box-helper (js/THREE.BoxHelper. object3d 0x00ff00)
        bounding-box (js/THREE.Box3.)
        move-increment 5
        _ ($! object3d :position.z 10)]
    (reify
      Object
      (updateBox [this]
        ($ box-helper update object3d)
        ($ bounding-box setFromObject box-helper))
      (moveLeft [this]
        ($ object3d translateX (- move-increment))
        (.updateBox this))
      (moveRight [this]
        ($ object3d translateX move-increment)
        (.updateBox this))
      (moveUp [this]
        ($ object3d translateY move-increment)
        (.updateBox this))
      (moveDown [this]
        ($ object3d translateY (- move-increment))
        (.updateBox this))
      (moveTo [this x y]
        (do
          ($! object3d :position.x x)
          ($! object3d :position.y y)
          (.updateBox this)))
      (getObject3d [this] object3d)
      (getBoundingBox [this] bounding-box)
      (getBoxHelper [this] box-helper))))

(defn origin
  []
  (let [geometry (js/THREE.PlaneGeometry. 5 5 1)
        material (js/THREE.MeshBasicMaterial. (clj->js {:color 0xff0000}))
        mesh (js/THREE.Mesh. geometry material)
        bounding-box (js/THREE.Box3.)
        object3d ($ (js/THREE.Object3D.) add mesh)
        box-helper (js/THREE.BoxHelper. object3d 0x00ff00)
        move-increment 5
        _ ($! object3d :position.x 0)
        _ ($! object3d :position.y 0)
        _ ($! object3d :position.z 1)]
    object3d))

(defn load-font!
  [url font-atom]
  ($ (js/THREE.FontLoader.)
     load
     url
     (fn [font]
       (reset! font-atom font))))

(defn table
  []
  (let [geometry (js/THREE.PlaneGeometry. 200 200 1)
        material (js/THREE.MeshBasicMaterial. (clj->js {:color 0xFFC0CB}))
        mesh (js/THREE.Mesh. geometry material)
        object3d ($ (js/THREE.Object3D.) add mesh)
        box-helper (js/THREE.BoxHelper. object3d 0x00ff00)
        bounding-box (js/THREE.Box3.)
        occupancy (atom 0)
        visited (atom 0)]
    (reify
      Object
      (getObject3d [this] object3d)
      (getBoundingBox [this] bounding-box)
      (getBoxHelper [this] box-helper)
      (getMaterial [this] material)
      (updateBox [this]
        ($ box-helper update object3d)
        ($ bounding-box setFromObject box-helper)
        ;;($! box-helper :visible false)
        )
      (intersectsBox [this box]
        ($ (.getBoundingBox this) intersectsBox box))
      (moveTo [this x y]
        (let [ ;; x-center (/ (- ($ bounding-box :max.x)
              ;;                ($ bounding-box :min.x))
              ;;             2)
              ;; y-center (/
              ;;           (- ($ bounding-box :max.y)
              ;;              ($ bounding-box :min.y))
              ;;           2)
              ]
          ;; ($! object3d :position.x (- x x-center))
          ;; ($! object3d :position.y (- y y-center))
          ($! object3d :position.x x)
          ($! object3d :position.y y)
          (.updateBox this)))
      (translate [this x y]
        ($ object3d translateX x)
        ($ object3d translateY y)
        (.updateBox this))
      (incrementOccupancy [this]
        (swap! occupancy inc))
      (resetOccupancy [this]
        (reset! occupancy 0))
      (getOccupancy [this]
        @occupancy)
      (incrementVisited [this]
        (swap! visited inc))
      (getTimesVisited [this]
        @visited)
      (setColor [this new-color]
        ($ material color.setHex new-color))
      (getColor [this]
        ($ material color.getHex)))))

(defn set-stage
  "Given a vector of vectors, return a vector of table in their proper places"
  [m]
  (let [table-width 200
        table-height 200
        total-width (* table-width (count (first m)))
        total-height (* table-height (count m))
        tables  (filter (comp not nil?)
                        (apply concat (map-indexed (fn [row itm]
                                                     (map-indexed (fn [col itm]
                                                                    (if (= itm 1)
                                                                      (let [table (table)]
                                                                        (.updateBox table)
                                                                        (.moveTo table (* col table-width) (* table-height row -1))
                                                                        table))) itm)) m)))
        group (js/THREE.Group.)
        ;; _ (doall (map #(.log js/console (.getObject3d %)) tables))
        _ (doall (map #($ group add (.getObject3d %)) tables))
        ;; bounding-box (js/THREE.Box3.)
        ;; _ ($ bounding-box setFromObject group)
        ;; x-center (/ (- ($ bounding-box :max.x)
        ;;                ($ bounding-box :min.x))
        ;;             2)
        ;; y-center (/
        ;;           (- ($ bounding-box :max.y)
        ;;              ($ bounding-box :min.y))
        ;;           2)
        ;; new-x (- 0 x-center)
        ;; new-y (- 0 y-center)
        translate-x (* (- (/ total-width 2) (/ table-width 2)) -1)
        translate-y (- (/ total-height 2) (/ table-height 2))
        ]
    ;; (.log js/console group)
    ;; (.log js/console x-center)
    ;; (.log js/console y-center)
    ;; (.log js/console new-x)
    ;; (.log js/console new-y)
    ;; (.log js/console ($ bounding-box :min))
    ;; (.log js/console ($ bounding-box :max))
    ;; (.log js/console ($ bounding-box :min.y))
    ;; (.log js/console ($ bounding-box :max.y))
    ;; (.log js/console ($ bounding-box :position))
    (doall (map (fn [table]
                  (.translate table translate-x translate-y)) tables))
    ;; (.log js/console width)
    ;; (.log js/console height)
    ;; ($ group translateX (+ (* (/ width 2) -1) 200))
    ;; ($ group translateY (/ height 2))
    ;; ($! group :position.x 0)
    ;; ($! group :position.y 0)
    ;; (.log js/console ($ group :position.x))
    ;; (.log js/console ($ group :position.y))
    ;; ($ group updateMatrix)
    ;; ($ group translateX (* (/ width 2) -1))
    ;; ($ group translateY (/ height 2))
    ;; ($! group :position.x 0)
    ;; ($! group :position.y 0)
    ;; (.log js/console group)
    ;;    group
    tables
    ))

(defn simple-plane
  "Return an object3d that represents a simple plane box centered at x,y"
  [x y]
  (let [geometry (js/THREE.PlaneGeometry. 20 20 1)
        material (js/THREE.MeshBasicMaterial. (clj->js {:color 0x00ff00}))
        mesh (js/THREE.Mesh. geometry material)
        object3d ($ (js/THREE.Object3D.) add mesh)]
    ($! object3d :position.x x)
    ($! object3d :position.y y)
    object3d))

(defn plane->bounding-box
  "Given an object3d that represents a simple plane, return a bounding box for it"
  [plane]
  (let [bounding-box (js/THREE.Box3.)]
    ($ bounding-box setFromObject plane)
    bounding-box))

(defn contains-point?
  "Does object's bounding box contain point?"
  [point object]
  ($ (.getBoundingBox object) containsPoint point))

(defn allowed-directions
  "Given the hero and the tables objects, determine which tables the hero can jump to"
  [hero tables]
  (let [table-width 200
        table-height 200
        hero-x ($ (.getObject3d hero) :position.x)
        hero-y ($ (.getObject3d hero) :position.y)
        top-left-point (js/THREE.Vector3. (- hero-x table-width)
                                          (+ hero-y table-height)
                                          0)
        top-right-point (js/THREE.Vector3.
                         (+ hero-x table-width)
                         (+ hero-y table-height)
                         0)
        bottom-left-point (js/THREE.Vector3. (- hero-x table-width)
                                             (- hero-y table-height)
                                             0)
        bottom-right-point (js/THREE.Vector3. (+ hero-x table-width)
                                              (- hero-y table-height)
                                              0)]
    {:top-left (first (filter (partial contains-point? top-left-point) tables))
     :top-right (first (filter (partial contains-point? top-right-point) tables))
     :bottom-left (first (filter (partial contains-point? bottom-left-point) tables))
     :bottom-right (first (filter (partial contains-point? bottom-right-point) tables))}))

(defn occupied-table
  "Given a hero and tables, return the table the hero is currently occupying"
  [hero tables]
  (let [hero-x ($ (.getObject3d hero) :position.x)
        hero-y ($ (.getObject3d hero) :position.y)]
    (first (filter (partial contains-point? (js/THREE.Vector3. hero-x hero-y 0)) tables))))

(defn reset-occupation!
  "Given a hero and tables, increment the occpancy of the tables
  that don't have the hero to 0,"
  [hero tables]
  (let [hero-x ($ (.getObject3d hero) :position.x)
        hero-y ($ (.getObject3d hero) :position.y)
        currently-occupied-table (occupied-table hero tables)
        non-occupied-tables (filter #(not= currently-occupied-table %) tables)]
    ;;(.log js/console currently-occupied-table)
    (doall (map #(.resetOccupancy %) non-occupied-tables))
    (.incrementOccupancy currently-occupied-table)
    (when (= (.getOccupancy currently-occupied-table) 1)
      (.incrementVisited currently-occupied-table))))

(defn set-colors!
  "Given the a col of table objects and the color cycle, set the tables to the proper colors"
  [color-cycle cycle? tables]
  (let [non-cycle-index (fn [value color-cycle]
                          (if (> value (- (count color-cycle) 1))
                            (- (count color-cycle) 1)
                            value))
        cycle-index (fn [value color-cycle]
                      (mod value (count color-cycle)))
        color-index (fn [value color-cycle cycle?]
                      (if cycle?
                        (cycle-index value color-cycle)
                        (non-cycle-index value color-cycle)))]
    (doall (map (fn [table] (.setColor table (color-cycle (color-index (.getTimesVisited table)
                                                                       color-cycle
                                                                       cycle?)))) tables))))
(defn game-won?
  [tables win-color]
  (every? true? (map #(= (.getColor %) win-color) tables)))

(defn game-won-fn
  []
  (menu/menu-screen
   state
   20
   (r/atom
    [{:id "play-again"
      :selected? true
      :on-click (fn [e]
                  (@(r/cursor state [:init-game])))}
     {:id "title-screen"
      :selected? false
      :on-click (fn [e]
                  (@(r/cursor state [:init-title-screen-fn])))}])))

(defn init-game-won-screen
  "The game is won, go to 'you win' screen"
  []
  (let [time-fn (r/cursor state [:time-fn])
        key-state (r/cursor state [:key-state])
        selected-menu-item (r/cursor state [:selected-menu-item])]
    (reset! key-state (:key-state initial-state))
    (reset! selected-menu-item "play-again")
    (reset! time-fn (game-won-fn))
    (r/render
     [GameWonScreen {:selected-menu-item selected-menu-item}]
     ($ js/document getElementById "reagent-app"))))

(defn init-game-lost-screen
  "The game is lost, go to 'Game Over' screen"
  []
  (let [time-fn (r/cursor state [:time-fn])
        key-state (r/cursor state [:key-state])
        selected-menu-item (r/cursor state [:selected-menu-item])]
    (reset! key-state (:key-state initial-state))
    (reset! selected-menu-item "play-again")
    (reset! time-fn (game-won-fn))
    (r/render
     [GameLostScreen {:selected-menu-item selected-menu-item}]
     ($ js/document getElementById "reagent-app"))))

(defn game-fn
  "The main game, as a fn of delta-t and state"
  []
  (let [hero (r/cursor state [:hero])
        enemy (r/cursor state [:enemy])
        ;;        goal (r/cursor state [:goal])
        tables (r/cursor state [:tables])
        render-fn (r/cursor state [:render-fn])
        key-state (r/cursor state [:key-state])
        paused? (r/cursor state [:paused?])
        key-state (r/cursor state [:key-state])
        ticks-max 20
        enemy-ticks (r/cursor state [:enemy-ticks])
        enemy-max-ticks 45
        p-ticks-counter (r/cursor state [:p-ticks-counter])
        up-ticks-counter (r/cursor state [:up-ticks-counter])
        right-ticks-counter (r/cursor state [:right-ticks-counter])
        down-ticks-counter (r/cursor state [:down-ticks-counter])
        left-ticks-counter (r/cursor state [:left-ticks-counter])
        hero-allowed-directions (r/cursor state [:hero-allowed-directions])
        enemy-allowed-directions (r/cursor state [:enemy-allowed-directions])
        move-hero! (fn [hero allowed-directions direction]
                     (if (direction allowed-directions)
                       (.moveTo hero
                                ($ (.getObject3d (direction allowed-directions))
                                   :position.x)
                                ($ (.getObject3d (direction allowed-directions))
                                   :position.y))))
        move-enemy! (fn [enemy table]
                      (.moveTo enemy
                               ($ (.getObject3d table) :position.x)
                               ($ (.getObject3d table) :position.y)))
        color-cycle (r/cursor state [:color-cycle])
        cycle-colors? (r/cursor state [:cycle-colors?])
        win-color (r/cursor state [:win-color])]
    (fn [delta-t]
      (@render-fn)
      #_      (when (.intersectsBox @goal (.getBoundingBox @hero))
                (init-game-won-screen))
      #_ (when (.intersectsBox @enemy (.getBoundingBox @hero))
           (init-game-lost-screen))
      ;; p-key is up, reset the delay
      (if (not (:p @key-state))
        (reset! p-ticks-counter 0))
      (if (and (not (:up-arrow @key-state))
               (not (:w @key-state)))
        (reset! up-ticks-counter 0))
      (if (and (not (:left-arrow @key-state))
               (not (:a @key-state)))
        (reset! left-ticks-counter 0))
      (if (and (not (:down-arrow @key-state))
               (not (:s @key-state)))
        (reset! down-ticks-counter 0))
      (if (and (not (:right-arrow @key-state))
               (not (:d @key-state)))
        (reset! right-ticks-counter 0))

      (reset-occupation! @hero @tables)
      (set-colors! @color-cycle @cycle-colors? @tables)
      ;; is the game won?
      (if (game-won? @tables @win-color)
        (init-game-won-screen))
      ;; set the allowed directions
      (reset! hero-allowed-directions (allowed-directions @hero @tables))
      (reset! enemy-allowed-directions (allowed-directions @enemy @tables))

      ;; move the hero when not paused
      (when-not @paused?
        (controls/key-down-handler
         @key-state
         {:up-fn (fn []
                   (controls/delay-repeat ticks-max up-ticks-counter #(move-hero! @hero @hero-allowed-directions :top-right)))
          :left-fn (fn []
                     (controls/delay-repeat ticks-max left-ticks-counter #(move-hero! @hero @hero-allowed-directions :top-left)))
          :down-fn (fn []
                     (controls/delay-repeat ticks-max down-ticks-counter #(move-hero! @hero @hero-allowed-directions :bottom-left)))
          :right-fn (fn []
                      (controls/delay-repeat ticks-max right-ticks-counter #(move-hero! @hero @hero-allowed-directions :bottom-right)))})
        ;; reset enemy ticks
        (when (< @enemy-ticks enemy-max-ticks)
          (swap! enemy-ticks inc))
        ;; ;; move the enemy
        (when (= @enemy-ticks enemy-max-ticks)
          (let [table-options (filter (comp not nil?) (vals @enemy-allowed-directions))]
            (move-enemy! @enemy (nth table-options (rand-int (count table-options))))
            (reset! enemy-ticks 0)))
        ;; is the game lost?
        (if ($ (.getBoundingBox @enemy) containsBox (.getBoundingBox @hero))
          (init-game-lost-screen)))
      ;; listen for the p-key depress
      (controls/key-down-handler
       @key-state
       {:p-fn (fn [] (controls/delay-repeat ticks-max p-ticks-counter
                                            #(reset! paused? (not @paused?))))}))))

(defn ^:export init-game
  "Function to setup and start the game"
  []
  (let [scene (js/THREE.Scene.)
        camera (display/init-camera!
                (display/create-perspective-camera
                 45
                 (/ ($ js/window :innerWidth)
                    ($ js/window :innerHeight))
                 0.1
                 20000)
                scene
                [0 0 3200])
        renderer (display/create-renderer)
        render-fn (display/render renderer scene camera)
        time-fn (r/cursor state [:time-fn])
        hero (hero)
        enemy (enemy)
        font-atom (r/cursor state [:font])
        tables (set-stage (:stage @state))
        paused? (r/cursor state [:paused?])
        key-state (r/cursor state [:key-state])
        key-state-tracker (r/cursor state [:key-state-tracker])
        enemy-ticks (r/cursor state [:enemy-ticks])]
    (swap! state assoc
           :render-fn render-fn
           :hero hero
           :enemy enemy
           :tables tables
           :scene scene
           :camera camera)
    (.updateBox hero)
    (.updateBox enemy)
    ($ scene add (.getObject3d hero))
    ($ scene add (.getBoxHelper hero))
    ($ scene add (.getObject3d enemy))
    ($ scene add (origin))
    (.moveTo hero 0 700)
    (.moveTo enemy 1200 -500)
    (reset! enemy-ticks 0)
    ;; (doall (mapv (fn [direction]
    ;;                ($ scene add direction))
    ;;              (allowed-directions hero)))
    ;; ($ scene add (.getObject3d goal))
    ;; ($ scene add (.getBoxHelper goal))
    ;; ($ scene add (.getObject3d table))
    ;; ($ scene add (.getBoxHelper table))
    (doall (map (fn [table]
                  (.setColor table (first (:color-cycle @state)))) tables))
    (doall (map (fn [table]
                  ;;(.log js/console (.getBoxHelper table))
                  ($ scene add (.getObject3d table))
                  ($ scene add (.getBoxHelper table))) tables))
    ;; ($ scene add (js/THREE.BoxHelper. tables))
    ;; ($! tables :position.x 0)
    ;; ($! tables :position.y 0)
    ;; ($ scene add tables)
    ;;    ($ scene add (.getObject3d enemy))
    ;;    ($ scene add (.getBoxHelper enemy))
    ;;    (.moveTo goal 0 -300)
    ;;    (.moveTo table 0 -300)
    ;;    (.moveTo enemy 50 400)
    (reset! time-fn (game-fn))
    (r/render
     [:div {:id "root-node"}
      [GameContainer {:renderer renderer
                      :camera camera
                      :state state}]
      [PauseComponent {:paused? paused?
                       :on-click (fn [event]
                                   (reset! paused? false))}]]
     ($ js/document getElementById "reagent-app"))))

(defn load-assets-fn
  []
  (let [font (r/cursor state [:font])]
    (fn [delta-t]
      (when-not (nil? @font)
        (init-game)))))

(defn load-game-assets
  []
  (let [font-url "fonts/helvetiker_regular.typeface.json"
        font-atom (r/cursor state [:font])
        time-fn (r/cursor state [:time-fn])]
    (load-font! font-url font-atom)
    (reset! time-fn (load-assets-fn))))

(defn title-screen-fn
  []
  (menu/menu-screen state
                    20
                    (r/atom
                     [{:id "start"
                       :selected? true
                       :on-click (fn [e]
                                   (load-game-assets))}
                      {:id "foo"
                       :selected? false
                       :on-click (fn [e]
                                   ($ js/console log "foo"))}
                      {:id "bar"
                       :selected? false
                       :on-click (fn [e]
                                   ($ js/console log "foo"))}])))

(defn ^:export init-title-screen
  []
  (let [time-fn (r/cursor state [:time-fn])
        selected-menu-item (r/cursor state [:selected-menu-item])
        key-state (r/cursor state [:key-state])]
    (reset! key-state (:key-state initial-state))
    (reset! selected-menu-item "start")
    ;; reset the time-fn
    (reset! time-fn (title-screen-fn))
    ;; mount the component
    (r/render
     [TitleScreen {:selected-menu-item selected-menu-item}]
     ($ js/document getElementById "reagent-app"))))

(defn ^:export init
  []
  (let [time-fn (r/cursor state [:time-fn])
        init-game-fn (r/cursor state [:init-game])
        key-state (r/cursor state [:key-state])
        init-game-won-fn (r/cursor state [:init-game-won-fn])
        init-title-screen-fn (r/cursor state [:init-title-screen-fn])]
    ;; start controls listeners
    (controls/initialize-key-listeners! key-state)
    ;; set init-fn's
    (reset! init-game-fn init-game)
    (reset! init-title-screen-fn init-title-screen)
    ;; start the loop
    (time-loop/start-time-loop time-fn)
    ;; initialize the title-screen
    (@init-title-screen-fn)))
