(ns greedy-pigeon.core
  (:require-macros [reagent.interop :refer [$ $!]])
  (:require [reagent.core :as r]
            [cljsjs.three]
            [cljsjs.soundjs]
            [howler]
            [greedy-pigeon.components :refer [AssetLoadingComponent PauseComponent TitleScreen GameContainer GameWonScreen GameLostScreen]]
            [greedy-pigeon.controls :as controls]
            [greedy-pigeon.display :as display]
            [greedy-pigeon.leaderboard :as leaderboard]
            [greedy-pigeon.menu :as menu]
            [greedy-pigeon.time-loop :as time-loop]
            [greedy-pigeon.utilities :as utilities]))

(def initial-state {:assets-loaded-percent nil
                    :paused? false
                    :died? false
                    :died-broom? false
                    :died-boot? false
                    :died-ticks nil
                    :key-state {}
                    :selected-menu-item "start"
                    :time-fn (constantly true)
                    :init-game (constantly true)
                    :init-play-again-fn (constantly true)
                    :init-title-screen-fn (constantly true)
                    :font nil
                    :stage
                    [[0 0 0 0 0 0 1 0 0 0 0 0 0]
                     [0 0 0 0 0 1 0 1 0 0 0 0 0]
                     [0 0 0 0 1 0 1 0 1 0 0 0 0]
                     [0 0 0 1 0 1 0 1 0 1 0 0 0]
                     [0 0 1 0 1 0 1 0 1 0 1 0 0]
                     [0 1 0 1 0 1 0 1 0 1 0 1 0]
                     [1 0 1 0 1 0 1 0 1 0 1 0 1]]
                    :table-height 200
                    :table-width 200
                    :table-decorations nil
                    :broom-offset 160
                    :hero-offset 90
                    :boot-offset 120
                    :broom-delay 100
                    :lives nil
                    :boot-ticks nil
                    :shadow-ticks nil
                    :shadow-offset 50
                    :lives-symbols nil
                    :score nil
                    :score-text nil
                    :total-score nil
                    :change-to-text nil
                    :change-to-table nil
                    :change-to-decoration nil
                    :background-overlay nil
                    :current-stage nil
                    :stage-text nil})

(defonce state (r/atom initial-state))

(def texture-urls ["images/table_red.png"
                   "images/pigeon_right_a.png"
                   "images/broom.png"
                   "images/boot_a.png"
                   "images/coins_small.png"
                   "images/coins_big.png"
                   "images/poop_small.png"
                   "images/poop_medium.png"
                   "images/poop_big.png"
                   "images/shadow_grey.png"
                   "images/shadow_black.png"
                   "images/cash_small.png"
                   "images/cash_lots.png"
                   "images/cash_and_coins.png"])

(def sound-urls ["audio/moveclick1.mp3"
                 "audio/moveclick2.mp3"
                 "audio/moveclick3.mp3"
                 "audio/moveclick4.mp3"
                 "audio/moveclick5.mp3"
                 "audio/no.mp3"
                 "audio/ohno1.mp3"
                 "audio/ooh_long.mp3"
                 "audio/oui.mp3"
                 "audio/oui_haha.mp3"
                 "audio/smash.mp3"
                 "audio/sweep.mp3"
                 "audio/youve_died.mp3"
                 "audio/ahha1.mp3"
                 "audio/ahha2.mp3"
                 "audio/gameover.mp3"
                 "audio/greedy_pigeon_theme.mp3"
                 ])

(def font-urls ["fonts/helvetiker_regular.typeface.json"])

(def stages
  [{:table-cycle ["cash-small" "none"]
    :cycle? false}
   {:table-cycle ["cash-small" "poop-medium"]
    :cycle? false}
   {:table-cycle ["cash-lots" "cash-small" "poop-medium"]
    :cycle? false}
   {:table-cycle ["cash-and-coins" "cash-lots" "cash-small" "none"]
    :cycle? false}
   {:table-cycle ["none" "poop-small" "poop-medium" "poop-big"]
    :cycle? false}
   {:table-cycle ["cash-and-coins" "coins-small" "none" "poop-medium"]
    :cycle? false}
   {:table-cycle ["none" "poop-medium"]
    :cycle? true}
   {:table-cycle ["cash-and-coins" "coins-small" "none" "poop-medium" "poop-big"]
    :cycle? false}
   {:table-cycle ["cash-and-coins" "coins-small" "cash-lots" "none" "poop-medium" "poop-big"]
    :cycle? false}
   {:table-cycle ["none" "poop-medium" "poop-big"]
    :cycle? true}])

(defn broom
  []
  (let [texture @(r/cursor state [:textures "broom.png"])
        geometry (js/THREE.PlaneGeometry. 80 240 1)
        material (js/THREE.MeshBasicMaterial.
                  (clj->js {:map texture
                            :side js/THREE.DoubleSide
                            :transparent true}))
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

(defn boot
  []
  (let [texture @(r/cursor state [:textures "boot_a.png"])
        geometry (js/THREE.PlaneGeometry. 140 160 1)
        material (js/THREE.MeshBasicMaterial.
                  (clj->js {:map texture
                            :side js/THREE.DoubleSide
                            :transparent true}))
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

(defn pigeon
  []
  (let [texture @(r/cursor state [:textures "pigeon_right_a.png"])
        geometry (js/THREE.PlaneGeometry. 150 150 1)
        material (js/THREE.MeshBasicMaterial.
                  (clj->js {:map texture
                            :side js/THREE.DoubleSide
                            :transparent true}))
        mesh (js/THREE.Mesh. geometry material)
        object3d ($ (js/THREE.Object3D.) add mesh)
        box-helper (js/THREE.BoxHelper. object3d 0x00ff00)
        bounding-box (js/THREE.Box3.)
        move-increment 5
        _ ($! object3d :position.z 10)
        ]
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

(defn table-decoration
  [texture height width]
  (let [geometry (js/THREE.PlaneGeometry. height width 1)
        material (js/THREE.MeshBasicMaterial.
                  (clj->js {:map texture
                            :side js/THREE.DoubleSide
                            :transparent true}))
        mesh (js/THREE.Mesh. geometry material)
        object3d ($ (js/THREE.Object3D.) add mesh)
        box-helper (js/THREE.BoxHelper. object3d 0x00ff00)
        bounding-box (js/THREE.Box3.)]
    (reify
      Object
      (updateBox [this]
        ($ box-helper update object3d)
        ($ bounding-box setFromObject box-helper))
      (getObject3d [this] object3d)
      (getBoundingBox [this] bounding-box)
      (moveTo [this x y]
        ($! object3d :position.x x)
        ($! object3d :position.y y)
        (.updateBox this)))))

(defn coins-small-decoration
  []
  (table-decoration @(r/cursor state [:textures "coins_small.png"])
                    120 60))

(defn cash-small-decoration
  []
  (table-decoration @(r/cursor state [:textures "cash_small.png"])
                    120 60))

(defn cash-lots-decoration
  []
  (table-decoration @(r/cursor state [:textures "cash_lots.png"])
                    120 60))

(defn cash-and-coins-decoration
  []
  (table-decoration @(r/cursor state [:textures "cash_and_coins.png"])
                    120 60))

(defn coins-big-decoration
  []
  (table-decoration @(r/cursor state [:textures "coins_big.png"])
                    120 60))

(defn poop-small-decoration
  []
  (table-decoration @(r/cursor state [:textures "poop_small.png"])
                    60 30))

(defn poop-medium-decoration
  []
  (table-decoration @(r/cursor state [:textures "poop_medium.png"])
                    100 50))

(defn poop-big-decoration
  []
  (table-decoration @(r/cursor state [:textures "poop_big.png"])
                    150 75))

(defn table
  []
  (let [texture @(r/cursor state [:textures "table_red.png"])
        geometry (js/THREE.PlaneGeometry. @(r/cursor state [:table-width]) @(r/cursor state [:table-height]) 1)
        material (js/THREE.MeshBasicMaterial. 
                  (clj->js {:map texture
                            :side js/THREE.DoubleSide
                            :transparent true}))
        mesh (js/THREE.Mesh. geometry material)
        object3d ($ (js/THREE.Object3D.) add mesh)
        box-helper (js/THREE.BoxHelper. object3d 0x00ff00)
        bounding-box (js/THREE.Box3.)
        occupancy (atom 0)
        visited (atom 0)
        decoration (atom nil)]
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
        (boolean ($ (.getBoundingBox this) intersectsBox box)))
      (moveTo [this x y]
        ($! object3d :position.x x)
        ($! object3d :position.y y)
        (.updateBox this))
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
      (resetVisited [this]
        (reset! visited 0))
      (getDecoration [this]
        @decoration)
      (setDecoration [this name]
        (reset! decoration name)))))

(defn text
  [font text & [color]]
  (let [geometry (js/THREE.TextGeometry. text
                                         (clj->js {:font @(r/cursor state [:fonts font])
                                                   :size 50
                                                   :height 10}))
        material (js/THREE.MeshBasicMaterial. (clj->js {:color (or color 0xD4AF37)}))
        mesh (js/THREE.Mesh. geometry material)
        object3d ($ (js/THREE.Object3D.) add mesh)
        box-helper (js/THREE.BoxHelper. object3d 0x00ff00)
        bounding-box (js/THREE.Box3.)]
    (reify
      Object
      (getObject3d [this] object3d)
      (getBoundingBox [this] bounding-box)
      (getBoxHelper [this] box-helper)
      (updateBox [this]
        ($ box-helper update object3d)
        ($ bounding-box setFromObject box-helper)
        ;;($! box-helper :visible false)
        )
      (intersectsBox [this box]
        ($ (.getBoundingBox this) intersectsBox box))
      (moveTo [this x y]
        ($! object3d :position.x x)
        ($! object3d :position.y y)
        (.updateBox this)))))

(defn play-sound
  [state filename]
  ($ @(r/cursor state [:sounds filename]) play))

(defn stop-sound
  [state filename]
  ($ @(r/cursor state [:sounds filename]) stop))

(defn init-game-container
  [state]
  (let [scene (js/THREE.Scene.)
        camera (display/init-camera!
                (display/create-perspective-camera
                 45
                 (/ ($ js/window :innerWidth)
                    ($ js/window :innerHeight))
                 0.1
                 20000)
                scene
                [0 0 2500])
        renderer (display/create-renderer)
        render-fn (display/render renderer scene camera)
        assets-loaded-percent (r/cursor state [:assets-loaded-percent])
        paused? (r/cursor state [:paused?])]
    ;; put the basic display elements into state
    (swap! state assoc
           :scene scene
           :camera camera
           :render-fn render-fn)
    ;; resize the view of the camera to match that of the viewport
    (display/window-resize! renderer camera)
    ;; add the game cointainer to the dom
    (r/render
     [:div {:id "root-node"}
      [GameContainer {:renderer renderer
                      :camera camera
                      :state state}]
      [PauseComponent {:paused? paused?
                       :on-click (fn [event]
                                   (reset! paused? false))}]
      [AssetLoadingComponent {:assets-loaded-percent assets-loaded-percent}]]
     ($ js/document getElementById "reagent-app"))))


(defn set-tables
  "Given a vector of vectors, return a vector of tables in their proper places"
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
        _ (doall (map #($ group add (.getObject3d %)) tables))
        translate-x (* (- (/ total-width 2) (/ table-width 2)) -1)
        translate-y (- (/ total-height 2) (/ table-height 2))]
    (doall (map (fn [table]
                  (.translate table translate-x translate-y)) tables))
    tables))

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

(defn instructions
  []
  (let [table-arrangement [[1 0 1]
                           [0 0 0]
                           [1 0 1]]
        tables (set-tables table-arrangement)
        pigeon (pigeon)
        group (js/THREE.Group.)
        up (text "helvetiker_regular.typeface.json" "W" 0xFFFFFF)
        left (text "helvetiker_regular.typeface.json" "A" 0xFFFFFF)
        down (text "helvetiker_regular.typeface.json" "S" 0xFFFFFF)
        right (text "helvetiker_regular.typeface.json" "D" 0xFFFFFF)]
    (.moveTo pigeon 1000 200)
    (.moveTo up 1160 425)
    (.moveTo left 770 425)
    (.moveTo down 770 20)
    (.moveTo right 1170 20)
    ($ group add (.getObject3d pigeon))
    (doall (map (fn [table] (.translate table 1000 200)) tables))
    (doall (map #($ group add (.getObject3d %)) tables))
    ($ group add (.getObject3d up))
    ($! (.getObject3d up) :position.z 15)
    ($! (.getObject3d left) :position.z 15)
    ($! (.getObject3d down) :position.z 15)
    ($! (.getObject3d right) :position.z 15)
    ($ group add (.getObject3d left))
    ($ group add (.getObject3d down))
    ($ group add (.getObject3d right))
    ($! group :position.z -2000)
    ($! group :position.x 1000)
    ($! group :position.y 300)
    group))

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
  "Given the hero and the tables objects, determine which tables the hero can jump to. Figure in an additional y-offset"
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
  "Given an object and the tables, return the table the object is currently occupying"
  [object tables]
  (let [object-x ($ (.getObject3d object) :position.x)
        object-y ($ (.getObject3d object) :position.y)
        object-z ($ (.getObject3d object) :position.z)
        object-bbox (.getBoundingBox object)
        this-bbox ($ object-bbox clone)]
    ;; because the tables lay within the z-plane
    ($ this-bbox translate (js/THREE.Vector3. 0 0 (- object-z)))
    (first (filter
            #(.intersectsBox % this-bbox)
            tables))))

(defn reset-occupation!
  "Given a hero and tables, increment the occpancy of the tables
  that don't have the hero to 0,"
  [hero tables]
  (let [hero-x ($ (.getObject3d hero) :position.x)
        hero-y ($ (.getObject3d hero) :position.y)
        currently-occupied-table (occupied-table hero tables)
        non-occupied-tables (filter #(not= currently-occupied-table %) tables)]
    (doall (map #(.resetOccupancy %) non-occupied-tables))
    (.incrementOccupancy currently-occupied-table)
    (when (= (.getOccupancy currently-occupied-table) 1)
      (.incrementVisited currently-occupied-table))))


(defn set-decorations-cycle!
  "Given the a col of table objects and the table cycle, set the tables to the proper cycle"
  [table-cycle cycle? tables]
  (let [non-cycle-index (fn [value table-cycle]
                          (if (> value (- (count table-cycle) 1))
                            (- (count table-cycle) 1)
                            value))
        cycle-index (fn [value table-cycle]
                      (mod value (count table-cycle)))
        decoration-index (fn [value table-cycle cycle?]
                           (if cycle?
                             (cycle-index value table-cycle)
                             (non-cycle-index value table-cycle)))]
    (doall (map (fn [table] (.setDecoration table (table-cycle (decoration-index (.getTimesVisited table)
                                                                                 table-cycle
                                                                                 cycle?)))) tables))))

(defn set-decoration!
  "Set the appropriate decoration on a table"
  [table]
  (let [table-x ($ (.getObject3d table) :position.x)
        table-y ($ (.getObject3d table) :position.y)
        scene (r/cursor state [:scene])
        decoration-name (.getDecoration table)
        table-decoration (condp = decoration-name
                           "none" nil
                           "coins-small" (coins-small-decoration)
                           "coins-big" (coins-big-decoration)
                           "poop-small" (poop-small-decoration)
                           "poop-medium" (poop-medium-decoration)
                           "poop-big" (poop-big-decoration)
                           "cash-small" (cash-small-decoration)
                           "cash-lots" (cash-lots-decoration)
                           "cash-and-coins" (cash-and-coins-decoration))]
    (when (not (nil? table-decoration))
      (.moveTo table-decoration table-x (+ table-y 50))
      ($ @scene add (.getObject3d table-decoration)))
    table-decoration))

(defn set-decorations!
  "Given the tables and current table-decorations, reset the table decorations"
  [tables table-decorations state]
  (let [scene @(r/cursor state [:scene])]
    ;; remove any table-decorations from the scene
    (when (not (empty? table-decorations))
      (doall (map #($ scene remove (.getObject3d %)) table-decorations)))
    ;; reset the table-decorations atom and add the
    ;; table decorations to the scene
    (reset! (r/cursor state [:table-decorations])
            (filter (comp not nil?)
             (doall (map set-decoration! tables))))))

(defn stage-won?
  [tables table-cycle]
  (every? true? (map #(= (.getDecoration %) (last table-cycle)) tables)))

(defn play-again-fn
  []
  (menu/menu-screen
   state
   20
   (r/atom
    [{:id "play-again"
      :selected? true
      :on-click (fn [e]
                  (init-game-container state)
                  (@(r/cursor state [:init-game])))}])))

(defn init-game-won-screen
  "The game is won, go to 'you win' screen"
  []
  (let [time-fn (r/cursor state [:time-fn])
        key-state (r/cursor state [:key-state])
        selected-menu-item (r/cursor state [:selected-menu-item])]
    (reset! key-state (:key-state initial-state))
    (reset! selected-menu-item "play-again")
    (reset! time-fn (play-again-fn))
    (r/render
     [GameWonScreen {:selected-menu-item selected-menu-item}]
     ($ js/document getElementById "reagent-app"))))

(defn init-game-lost-screen
  "The game is lost, go to 'Game Over' screen"
  [url]
  (let [time-fn (r/cursor state [:time-fn])
        key-state (r/cursor state [:key-state])
        selected-menu-item (r/cursor state [:selected-menu-item])
        score (r/cursor state [:score])
        current-stage (r/cursor state [:current-stage])]
    (reset! key-state (:key-state initial-state))
    (reset! selected-menu-item "play-again")
    (reset! time-fn (play-again-fn))
    (r/render
     [GameLostScreen {:selected-menu-item selected-menu-item
                      :url url
                      :stage (+ 1 @current-stage)
                      :score @score}]
     ($ js/document getElementById "reagent-app"))))

(defn show-lives!
  [state]
  (let [lives-symbols (r/cursor state [:lives-symbols])
        lives (r/cursor state [:lives])
        scene (r/cursor state [:scene])]
    ;; remove any symbols that are present
    (doall (map #($ @scene remove (.getObject3d %)) @lives-symbols))
    ;; create the symbols
    (reset! lives-symbols (take @lives (repeatedly pigeon)))
    ;; move the symbols to the proper place
    (doall (map-indexed (fn [idx pigeon]
                          ($ @scene add (.getObject3d pigeon))
                          (.moveTo pigeon (+ 1200 (* idx -150)) 700))
                        @lives-symbols))))

(defn shadow-chase-hero!
  [state]
  (let [hero (r/cursor state [:hero])
        tables (r/cursor state [:tables])
        shadow (r/cursor state [:shadow])
        shadow-offset (r/cursor state [:shadow-offset])
        hero-table (occupied-table @hero @tables)
        table-x ($ (.getObject3d hero-table) :position.x)
        table-y ($ (.getObject3d hero-table) :position.y)]
    (.moveTo @shadow table-x (+ table-y @shadow-offset))))

(defn reset-boot!
  [state]
  (let [boot (r/cursor state [:boot])
        boot-ticks (r/cursor state [:boot-ticks])
        shadow (r/cursor state [:shadow])
        shadow-ticks (r/cursor state [:shadow-ticks])
        hero (r/cursor state [:hero])]
    (reset! shadow-ticks 0)
    (reset! boot-ticks 0)
    (.moveTo @shadow 0 10000)
    (.moveTo @boot 0 10000)))

(defn calculate-score
  "calculate score"
  [state]
  (let [table-cycle (r/cursor state [:table-cycle])
        tables (r/cursor state [:tables])
        score (atom 0)]
    (apply + (map-indexed (fn [idx itm]
                            (* idx (count (filter #(= (.getDecoration %)
                                                      itm) @tables)))) @table-cycle))))
(defn reset-score!
  "reset the score and redraw its value"
  [state new-score]
  (let [score (r/cursor state [:score])
        score-text (r/cursor state [:score-text])
        scene (r/cursor state [:scene])]
    (reset! score new-score)
    ($ @scene remove (.getObject3d @score-text))
    (reset! score-text (text "helvetiker_regular.typeface.json" @score))
    (.moveTo @score-text -1200 700)
    ($ @scene add (.getObject3d @score-text))))

(defn reset-displayed-stage!
  "reset the current displayed stage, showing which one we are on"
  [n]
  (let [current-stage (r/cursor state [:current-stage])
        stage-text (r/cursor state [:stage-text])
        scene (r/cursor state [:scene])]
    (reset! current-stage n)
    (when ((comp not nil?) @stage-text)
      ($ @scene remove (.getObject3d @stage-text)))
    (reset! stage-text (text "helvetiker_regular.typeface.json" (str "Stage " (+ @current-stage 1))))
    (.moveTo @stage-text -1200 600)
    ($ @scene add (.getObject3d @stage-text))))

(defn set-stage!
  [n]
  (let [table-cycle (r/cursor state [:table-cycle])
        cycle? (r/cursor state [:cycle?])
        current-stage (r/cursor state [:current-stage])
        stage (nth stages n)]
    (reset! current-stage n)
    (reset! table-cycle (:table-cycle stage))
    (reset! cycle? (:cycle? stage))
    (reset-displayed-stage! n)))

(defn next-stage
  []
  (let [current-stage (r/cursor state [:current-stage])
        next-stage (+ @current-stage 1)]
    (if (> next-stage (- (count stages) 1))
      0
      next-stage)))

(defn init-stage
  [state]
  (let [scene (r/cursor state [:scene])
        hero (r/cursor state [:hero])
        broom (r/cursor state [:broom])
        boot (r/cursor state [:boot])
        shadow (r/cursor state [:shadow])
        tables (r/cursor state [:tables])
        paused? (r/cursor state [:paused?])
        died? (r/cursor state [:died?])
        died-ticks (r/cursor state [:died-ticks])
        key-state (r/cursor state [:key-state])
        key-state-tracker (r/cursor state [:key-state-tracker])
        broom-ticks (r/cursor state [:broom-ticks])
        shadow-ticks (r/cursor state [:shadow-ticks])
        boot-ticks (r/cursor state [:boot-ticks])
        broom-offset (r/cursor state [:broom-offset])
        boot-offset (r/cursor state [:boot-offset])
        hero-offset (r/cursor state [:hero-offset])
        table-decorations (r/cursor state [:table-decorations])
        lives (r/cursor state [:lives])
        score (r/cursor state [:score])
        score-text (r/cursor state [:score-text])
        change-to-text (r/cursor state [:change-to-text])
        change-to-table (r/cursor state [:change-to-table])
        change-to-decoration (r/cursor state [:change-to-decoration])
        table-cycle (r/cursor state [:table-cycle])]
    ;; reset the key-state
    (reset! key-state (:key-state initial-state))
    ;; update the hero, broom and boot boxes
    (.updateBox @hero)
    (.updateBox @broom)
    (.updateBox @boot)
    ;; move the hero to the proper place
    (.moveTo @hero 0 700)
    ;; move the broom to the proper place
    (.moveTo @broom
             1200 ;;-480
             (+ -600 @broom-offset)
             )
    ;; the shadow needs to be move to the proper place
    ($! (.getObject3d @shadow) :position.z 9)
    (.moveTo @shadow 0
             (+ 600 @(r/cursor state [:shadow-offset])))
    ;; move the boot to the proper place
    (.moveTo @boot
             0
             10000)
    ;; set the ticks properly
    (reset! broom-ticks 0)
    (reset! shadow-ticks 0)
    (reset! boot-ticks 0)
    (reset! died-ticks 0)
    ;; make sure we aren't dead
    (reset! died? false)
    ;; show our lives total
    (show-lives! state)
    ;; show the current score
    (reset-score! state @score)
    ;; show the change to menu table
    (reset! change-to-table (table))
    (.moveTo @change-to-table -1000 200)
    ($ @scene add (.getObject3d @change-to-table))
    ;; set the decorations to win-con
    (.setDecoration @change-to-table (last @table-cycle))
    ;; show the change to decoration
    (reset! change-to-decoration (set-decoration! @change-to-table))
    ;; initialize table
    (doall (map #(.setDecoration % (first (:table-cycle @state))) @tables))
    (doall (map #(.resetVisited %) @tables))
    ;; add the new ones in
    (set-decorations! @tables @table-decorations state)))

(defn game-fn
  "The main game, as a fn of delta-t and state"
  []
  (let [hero (r/cursor state [:hero])
        broom (r/cursor state [:broom])
        tables (r/cursor state [:tables])
        table-decorations (r/cursor state [:table-decorations])
        render-fn (r/cursor state [:render-fn])
        key-state (r/cursor state [:key-state])
        paused? (r/cursor state [:paused?])
        died? (r/cursor state [:died?])
        died-ticks (r/cursor state [:died-ticks])
        died-ticks-max 100
        key-state (r/cursor state [:key-state])
        ticks-max 20
        broom-ticks (r/cursor state [:broom-ticks])
        broom-max-ticks 45
        broom-delay (r/cursor state [:broom-delay])
        shadow-ticks-max 100
        shadow-ticks (r/cursor state [:shadow-ticks])
        shadow (r/cursor state [:shadow])
        boot (r/cursor state [:boot])
        boot-ticks-max 99
        boot-ticks (r/cursor state [:boot-ticks])
        space-ticks-counter (r/cursor state [:space-ticks-counter])
        up-ticks-counter (r/cursor state [:up-ticks-counter])
        right-ticks-counter (r/cursor state [:right-ticks-counter])
        down-ticks-counter (r/cursor state [:down-ticks-counter])
        left-ticks-counter (r/cursor state [:left-ticks-counter])
        hero-allowed-directions (r/cursor state [:hero-allowed-directions])
        broom-allowed-directions (r/cursor state [:broom-allowed-directions])
        hero-offset (r/cursor state [:hero-offset])
        broom-offset (r/cursor state [:broom-offset])
        boot-offset (r/cursor state [:boot-offset])
        shadow-offset (r/cursor state [:shadow-offset])
        total-score (r/cursor state [:total-score])
        score (r/cursor state [:score])
        move-hero! (fn [hero allowed-directions direction]
                     (when (direction allowed-directions)
                       ;; move hero
                       (.moveTo hero
                                ($ (.getObject3d (direction allowed-directions))
                                   :position.x)
                                (+  ($ (.getObject3d (direction allowed-directions)) :position.y)
                                    @hero-offset))
                       ;; redraw the table decorations
                       (set-decorations! @tables @table-decorations state)
                       ;; reset the score
                       (reset-score! state (+ @total-score (calculate-score state)))
                       (play-sound state "moveclick5.mp3")))
        move-broom! (fn [broom table]
                      (.moveTo broom
                               ($ (.getObject3d table) :position.x)
                               (+  ($ (.getObject3d table) :position.y)
                                   @broom-offset)))
        table-cycle (r/cursor state [:table-cycle])
        cycle? (r/cursor state [:cycle?])
        lives (r/cursor state [:lives])
        died? (r/cursor state [:died?])
        current-stage (r/cursor state [:current-stage])]
    (fn [delta-t]
      (@render-fn)
      ;; p-key is up, reset the delay
      (if (not (:space @key-state))
        (reset! space-ticks-counter 0))
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
      ;; set the tables occupation and decorations
      (reset-occupation! @hero @tables)
      (set-decorations-cycle! @table-cycle @cycle? @tables)
      ;; is the stage won?
      (when (stage-won? @tables @table-cycle)
        ;; set to the next stage
        (set-stage! (next-stage))
        (reset! total-score @score)
        (play-sound state "oui_haha.mp3")
        (init-stage state))
      ;; set the allowed directions
      (reset! hero-allowed-directions (allowed-directions (occupied-table @hero @tables) @tables))
      (reset! broom-allowed-directions (allowed-directions (occupied-table @broom @tables) @tables))
      ;; pause the game momentarily when you die
      (when (and @died? (not @paused?))
        (swap! died-ticks inc))
      ;; restart the game after died-ticks-max
      (when (and (= @died-ticks died-ticks-max)
                 (not @paused?))
        ;; move the hero away from the broom
        (when (= (occupied-table @hero @tables)
                 (occupied-table @broom @tables))
          (let [table-options (filter (comp not nil?) (vals @broom-allowed-directions))]
            (.moveTo @hero
                     ($ (.getObject3d (first table-options))
                        :position.x)
                     (+  ($ (.getObject3d (first table-options))
                            :position.y)
                         @hero-offset))))
        (reset! broom-ticks 0)
        (reset-boot! state)
        (shadow-chase-hero! state)
        (reset! died? false)
        (reset! died-ticks 0))
      ;; move the hero when not paused
      (when-not (or @paused? @died?)
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
        ;; reset broom ticks
        (when (< @broom-ticks broom-max-ticks)
          (swap! broom-ticks inc))
        ;; ;; move the broom
        (when (= @broom-ticks broom-max-ticks)
          (let [table-options (filter (comp not nil?) (vals @broom-allowed-directions))]
            (move-broom! @broom (nth table-options (rand-int (count table-options))))
            (reset! broom-ticks 0)))
        ;; reset shadow ticks
        (when (< @shadow-ticks shadow-ticks-max)
          (swap! shadow-ticks inc))
        ;; move the shadow to where the hero is
        (when (= @shadow-ticks shadow-ticks-max)
          (shadow-chase-hero! state)
          (reset! shadow-ticks 0))
        ;; reset boot ticks
        (when (< @boot-ticks boot-ticks-max)
          (swap! boot-ticks inc))
        ;; move the boot to where the shadow is
        (when (= @boot-ticks boot-ticks-max)
          (let [shadow-table (occupied-table @shadow @tables)]
            (if (not (nil? shadow-table))
              (let [table-x ($ (.getObject3d shadow-table) :position.x)
                    table-y ($ (.getObject3d shadow-table) :position.y)]
                (.moveTo @boot table-x (+ table-y @boot-offset))
                (reset! boot-ticks 0)))))

        ;; is the game lost?
        (when (= (occupied-table @hero @tables)
                 (occupied-table @broom @tables))
          (when (= @lives 0)
            (stop-sound state "greedy_pigeon_theme.mp3")
            (play-sound state "gameover.mp3")
            (reset! died? true)
            (init-game-lost-screen "images/broomed.png"))
          (when (> @lives 0)
            (play-sound state "ohno1.mp3")
            (swap! lives dec)
            (show-lives! state)
            (reset! died? true)))
        (when (= (occupied-table @hero @tables)
                 (occupied-table @boot @tables))
          (when (= @lives 0)
            (stop-sound state "greedy_pigeon_theme.mp3")
            (play-sound state "gameover.mp3")
            (reset! died? true)
            (init-game-lost-screen "images/smashed.png"))
          (when (> @lives 0)
            (play-sound state "no.mp3")
            (swap! lives dec)
            (show-lives! state)
            (reset! died? true))))
      ;; listen for the p-key depress
      (controls/key-down-handler
       @key-state
       {:space-fn (fn [] (controls/delay-repeat ticks-max space-ticks-counter
                                                #(reset! paused? (not @paused?))))}))))


(defn ^:export init-game
  "Function to setup and start the game"
  [state]
  (let [scene (r/cursor state [:scene])
        time-fn (r/cursor state [:time-fn])
        hero (pigeon)
        broom (broom)
        boot (boot)
        tables (set-tables (:stage @state))
        shadow (table-decoration @(r/cursor state [:textures "shadow_black.png"]) 130 22)
        lives (r/cursor state [:lives])
        score (r/cursor state [:score])
        score-text (r/cursor state [:score-text])
        change-to-text (r/cursor state [:change-to-text])
        total-score (r/cursor state [:total-score])
        instructions (instructions)]
    (stop-sound state "gameover.mp3")
    (stop-sound state "greedy_pigeon_theme.mp3")
    (play-sound state "greedy_pigeon_theme.mp3")
    (swap! state assoc
           :hero hero
           :broom broom
           :boot boot
           :tables tables
           :shadow shadow)
    (.updateBox hero)
    (.updateBox broom)
    (.updateBox boot)
    ($ @scene add (.getObject3d hero))
    ($ @scene add (.getObject3d broom))
    ($ @scene add (.getObject3d boot))
    ($ @scene add (.getObject3d shadow))
    (reset! lives 3)
    (reset! score 0)
    (reset! total-score 0)
    ;; add the tables to the scene
    (doall (map (fn [table]
                  ($ @scene add (.getObject3d table))) tables))
    ;; show lives total
    (show-lives! state)
    ;; set the score text
    (reset! score-text (text "helvetiker_regular.typeface.json" @score))
    ;; show the change to menu text
    (reset! change-to-text (text "helvetiker_regular.typeface.json" "Change To"))
    (.moveTo @change-to-text -1170 350)
    ($ @scene add (.getObject3d @change-to-text))
    (reset! time-fn (game-fn))
    ;; set the proper stage
    (set-stage! 0)
    (init-stage state)
    ;; add the instructions
    ($ @scene add instructions)))

(defn percent-assets-loaded
  "Return the total amount of assets that has been loaded"
  []
  (let [textures (r/cursor state [:textures])
        sounds (r/cursor state [:sounds])
        fonts (r/cursor state [:fonts])
        percent-textures (/ (count @textures)
                            (count texture-urls))
        percent-sounds (/ (count (filter true? (map #(= ($ % state) "loaded") (vals @sounds))))
                          (count sound-urls))
        percent-fonts (/ (count @fonts)
                         (count font-urls))]
    (/ (+ percent-textures
          percent-sounds
          percent-fonts)
       3)))

(defn load-assets-fn
  []
  (let [assets-loaded-percent (r/cursor state [:assets-loaded-percent])]
    (fn [delta-t]
      (reset! assets-loaded-percent (percent-assets-loaded))
      (when (=  @assets-loaded-percent 1)
        (init-game state)))))

(defn sound-loader
  [state url]
  (let [sounds (r/cursor state [:sounds])
        sound (js/Howl. (clj->js {:src [url]}))]
    (swap! sounds assoc (utilities/url->filename url) sound)))

(defn font-loader
  [state url]
  (let [fonts (r/cursor state [:fonts])]
    ($ (js/THREE.FontLoader.)
       load
       url
       (fn [font]
         (swap! fonts assoc (utilities/url->filename url) font)))))

(defn texture-loader
  [state url]
  (let [textures (r/cursor state [:textures])
        loader (js/THREE.TextureLoader.)]
    ($ loader load
       url
       (fn [texture]
         (swap! textures assoc
                (utilities/url->filename url) texture))
       ;; onLoad and onProgress don't work in THREE.js
       ;; but kept because they appear in
       ;; https://threejs.org/docs/#api/loaders/TextureLoader
       ;; for reasons why, see
       ;; see: https://github.com/mrdoob/three.js/issues/7734
       ;;      https://github.com/mrdoob/three.js/issues/10439
       (fn [xhr]
         (.log js/console (str (* 100 (/ ($ xhr :loaded)
                                         ($ xhr :total)))) url "% loaded"))
       (fn [xhr]
         (.log js/console "An error occured when loaded " url)))))

(defn load-game-assets
  []
  (let [time-fn (r/cursor state [:time-fn])
        sounds (r/cursor state [:sounds])
        assets-loaded-percent (r/cursor state [:assets-loaded-percent])]
    (reset! assets-loaded-percent 0)
    (doall (map (partial font-loader state) font-urls))
    (doall (map (partial texture-loader state) texture-urls))
    (when ((comp not nil?) @sounds)
      (doall (map #($ % unload) (vals @sounds))))
    (doall (map (partial sound-loader state) sound-urls))
    (reset! time-fn (load-assets-fn))))


(defn title-screen-fn
  []
  (menu/menu-screen state
                    20
                    (r/atom
                     [{:id "start"
                       :selected? true
                       :on-click (fn [e]
                                   (init-game-container state)
                                   (load-game-assets))}])))

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
        init-play-again-fn (r/cursor state [:init-play-again-fn])
        init-title-screen-fn (r/cursor state [:init-title-screen-fn])]
    ;; start controls listeners
    (controls/initialize-key-listeners! key-state)
    ;; set init-fn's
    (reset! init-game-fn (partial init-game state))
    (reset! init-title-screen-fn init-title-screen)
    ;; start the loop
    (time-loop/start-time-loop time-fn)
    ;; initialize the title-screen
    (@init-title-screen-fn)))
