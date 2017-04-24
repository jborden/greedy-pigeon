(ns greedy-pigeon.core
  (:require-macros [reagent.interop :refer [$ $!]])
  (:require [reagent.core :as r]
            [cljsjs.three]
            [cljsjs.soundjs]
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
                    :table-cycle ;; ["none" "coins-small" "coins-big" "poop-small" "poop-medium" "poop-big"]
                    ["none" "coins-small" "coins-big"]
                    :win-con "coins-big"
                    ;; "poop-big"
                    :cycle-colors? false
                    :stage [[0 0 0 0 0 0 1 0 0 0 0 0 0]
                            [0 0 0 0 0 1 0 1 0 0 0 0 0]
                            [0 0 0 0 1 0 1 0 1 0 0 0 0]
                            [0 0 0 1 0 1 0 1 0 1 0 0 0]
                            [0 0 1 0 1 0 1 0 1 0 1 0 0]
                            [0 1 0 1 0 1 0 1 0 1 0 1 0]
                            [1 0 1 0 1 0 1 0 1 0 1 0 1]]
                    :hop "Hop"
                    :table-texture nil
                    :hero-texture nil
                    :broom-texture nil
                    :coins-small-texture nil
                    :coins-big-texture nil
                    :poop-small-texture nil
                    :poop-medium-texture nil
                    :poop-big-texture nil
                    :table-decorations nil
                    :broom-offset 160
                    :hero-offset 90
                    })

(defonce state (r/atom initial-state))

(defn broom
  []
  (let [texture @(r/cursor state [:broom-texture])
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

(defn hero
  []
  (let [texture @(r/cursor state [:hero-texture])
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

(defn load-font!
  [url font-atom]
  ($ (js/THREE.FontLoader.)
     load
     url
     (fn [font]
       (reset! font-atom font))))

(defn table-decoration
  [texture height width]
  (let [geometry (js/THREE.PlaneGeometry. height width 1)
        material (js/THREE.MeshBasicMaterial.
                  (clj->js {:map texture
                            :side js/THREE.DoubleSide
                            :transparent true}))
        mesh (js/THREE.Mesh. geometry material)
        object3d ($ (js/THREE.Object3D.) add mesh)]
    (reify
      Object
      (getObject3d [this] object3d)
      (moveTo [this x y]
        ($! object3d :position.x x)
        ($! object3d :position.y y)))))

(defn coins-small-decoration
  []
  (table-decoration (:coins-small-texture @state)
                    120 60))

(defn coins-big-decoration
  []
  (table-decoration (:coins-big-texture @state)
                    120 60))

(defn poop-small-decoration
  []
  (table-decoration (:poop-small-texture @state)
                    120 60))

(defn poop-medium-decoration
  []
  (table-decoration (:poop-medium-texture @state)
                    120 60))

(defn poop-big-decoration
  []
  (table-decoration (:poop-big-texture @state)
                    120 60))

(defn table
  []
  (let [texture @(r/cursor state [:table-texture])
        geometry (js/THREE.PlaneGeometry. 200 200 1)
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
      (getDecoration [this]
        @decoration)
      (setDecoration [this name]
        (reset! decoration name)))))

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
    ;;(.log js/console object-bbox)
    ;;(.log js/console (clj->js tables))
    ;;(partial contains-point? (js/THREE.Vector3. object-x object-y 0))
    (first (filter
            #(.intersectsBox % this-bbox)
            ;;(partial contains-point? (js/THREE.Vector3. object-x object-y 0))
            tables))))

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

(defn set-decorations!
  "Given the tables and current table-decorations, reset the table decorations"
  [tables table-decorations state]
  (let [scene @(r/cursor state [:scene])]
;;    (.log js/console (clj->js table-decorations))
    ;; remove any table-decorations from the scene
    (when (not (empty? table-decorations))
      (doall (map #($ scene remove (.getObject3d %)) table-decorations)))
    ;; reset the table-decorations atom and add the
    ;; table decorations to the scene
    (reset! (r/cursor state [:table-decorations])
            (filter (comp not nil?)
             (doall (map (fn [table]
                           (let [table-x ($ (.getObject3d table) :position.x)
                                 table-y ($ (.getObject3d table) :position.y)
                                 decoration-name (.getDecoration table)
                                 table-decoration (condp = decoration-name
                                                    "none" nil
                                                    "coins-small" (coins-small-decoration)
                                                    "coins-big" (coins-big-decoration)
                                                    "poop-small" (poop-small-decoration)
                                                    "poop-medium" (poop-medium-decoration)
                                                    "poop-big" (poop-big-decoration))]
                             (when (not (nil? table-decoration))
                               (.moveTo table-decoration table-x (+ table-y 50))
                               ($ scene add (.getObject3d table-decoration)))
                             table-decoration)) tables))))))

(defn game-won?
  [tables win-con]
  (every? true? (map #(= (.getDecoration %) win-con) tables)))

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
        broom (r/cursor state [:broom])
        ;;        goal (r/cursor state [:goal])
        tables (r/cursor state [:tables])
        table-decorations (r/cursor state [:table-decorations])
        render-fn (r/cursor state [:render-fn])
        key-state (r/cursor state [:key-state])
        paused? (r/cursor state [:paused?])
        key-state (r/cursor state [:key-state])
        ticks-max 20
        broom-ticks (r/cursor state [:broom-ticks])
        broom-max-ticks 45
        p-ticks-counter (r/cursor state [:p-ticks-counter])
        up-ticks-counter (r/cursor state [:up-ticks-counter])
        right-ticks-counter (r/cursor state [:right-ticks-counter])
        down-ticks-counter (r/cursor state [:down-ticks-counter])
        left-ticks-counter (r/cursor state [:left-ticks-counter])
        hero-allowed-directions (r/cursor state [:hero-allowed-directions])
        broom-allowed-directions (r/cursor state [:broom-allowed-directions])
        hero-offset (r/cursor state [:hero-offset])
        broom-offset (r/cursor state [:broom-offset])
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
                       ($ js/createjs Sound.play (:hop @state))))
        move-broom! (fn [broom table]
                      (.moveTo broom
                               ($ (.getObject3d table) :position.x)
                               (+  ($ (.getObject3d table) :position.y)
                                   @broom-offset)))
        table-cycle (r/cursor state [:table-cycle])
        cycle-colors? (r/cursor state [:cycle-colors?])
        win-con (r/cursor state [:win-con])]
    (fn [delta-t]
      (@render-fn)
      #_      (when (.intersectsBox @goal (.getBoundingBox @hero))
                (init-game-won-screen))
      #_ (when (.intersectsBox @broom (.getBoundingBox @hero))
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
      ;; set the tables occupation and decorations
      (reset-occupation! @hero @tables)
      (set-decorations-cycle! @table-cycle @cycle-colors? @tables)
      ;;
      ;; is the game won?
      (if (game-won? @tables @win-con)
        (init-game-won-screen))
      ;; set the allowed directions
      (reset! hero-allowed-directions (allowed-directions (occupied-table @hero @tables) @tables))
      (reset! broom-allowed-directions (allowed-directions (occupied-table @broom @tables) @tables))

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
        ;; reset broom ticks
        (when (< @broom-ticks broom-max-ticks)
          (swap! broom-ticks inc))
        ;; ;; move the broom
        (when (= @broom-ticks broom-max-ticks)
          (let [table-options (filter (comp not nil?) (vals @broom-allowed-directions))]
            (move-broom! @broom (nth table-options (rand-int (count table-options))))
            (reset! broom-ticks 0)))
        ;; is the game lost?
        (if ;;($ (.getBoundingBox @broom) containsBox (.getBoundingBox @hero))
            (= (occupied-table @hero @tables)
               (occupied-table @broom @tables))
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
                [0 0 2500])
        renderer (display/create-renderer)
        render-fn (display/render renderer scene camera)
        time-fn (r/cursor state [:time-fn])
        hero (hero)
        broom (broom)
        font-atom (r/cursor state [:font])
        tables (set-stage (:stage @state))
        paused? (r/cursor state [:paused?])
        key-state (r/cursor state [:key-state])
        key-state-tracker (r/cursor state [:key-state-tracker])
        broom-ticks (r/cursor state [:broom-ticks])
        broom-offset (r/cursor state [:broom-offset])
        hero-offset (r/cursor state [:hero-offset])
        table-decorations (r/cursor state [:table-decorations])]
    (swap! state assoc
           :render-fn render-fn
           :hero hero
           :broom broom
           :tables tables
           :scene scene
           :camera camera)
    (.updateBox hero)
    (.updateBox broom)
    ($ scene add (.getObject3d hero))
;;;    ($ scene add (.getBoxHelper hero))
    ($ scene add (.getObject3d broom))
    ($ scene add (origin))
    ;;(.moveTo hero 0 690)
    (.moveTo hero 0 ;;700
             700
             )
    (.moveTo broom 1200 ;;-480
             (+ -600 @broom-offset))
    (reset! broom-ticks 0)
    (doall (map (fn [table]
                  ;;(.log js/console (.getBoxHelper table))
                  ($ scene add (.getObject3d table))
                  ;;            ($ scene add (.getBoxHelper table))
                  ) tables))
    ;; ($ scene add (js/THREE.BoxHelper. tables))
    ;; initial table decorations
    (doall (map #(.setDecoration % (first (:table-cycle @state))) tables))
    (set-decorations! tables @table-decorations state)
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

(defn load-sound!
  []
  ($ js/createjs Sound.registerSound "audio/bounce.mp3" (:hop @state)))

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
        time-fn (r/cursor state [:time-fn])
        table-texture (r/cursor state [:table-texture])
        hero-texture (r/cursor state [:hero-texture])
        broom-texture (r/cursor state [:broom-texture])
        coins-small-texture (r/cursor state [:coins-small-texture])
        coins-big-texture (r/cursor state [:coins-big-texture])
        poop-small-texture (r/cursor state [:poop-small-texture])
        poop-medium-texture (r/cursor state [:poop-medium-texture])
        poop-big-texture (r/cursor state [:poop-big-texture])]
    (reset! table-texture (js/THREE.ImageUtils.loadTexture. "images/table_red.png"))
    (reset! hero-texture (js/THREE.ImageUtils.loadTexture. "images/pigeon_right_a.png"))
    (reset! broom-texture (js/THREE.ImageUtils.loadTexture. "images/broom.png"))
    (reset! coins-small-texture (js/THREE.ImageUtils.loadTexture. "images/coins_small.png"))
    (reset! coins-big-texture (js/THREE.ImageUtils.loadTexture. "images/coins_big.png"))
    (reset! poop-small-texture (js/THREE.ImageUtils.loadTexture. "images/poop_small.png"))
    (reset! poop-medium-texture (js/THREE.ImageUtils.loadTexture. "images/poop_medium.png"))
    (reset! poop-big-texture (js/THREE.ImageUtils.loadTexture. "images/poop_big.png"))
    (load-font! font-url font-atom)
    (load-sound!)
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
