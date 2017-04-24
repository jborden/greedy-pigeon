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
                    :table-cycle ;; ["none" "coins-small" "coins-big" "poop-small" "poop-medium" "poop-big"]
                    nil
                    ;;["none" "coins-small" "coins-big"]
                    :win-con ;;"coins-big"
                    nil
                    ;; "poop-big"
                    :cycle? false
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
                    :hop "Hop"
                    :table-texture nil
                    :hero-texture nil
                    :broom-texture nil
                    :coins-small-texture nil
                    :coins-big-texture nil
                    :poop-small-texture nil
                    :poop-medium-texture nil
                    :poop-big-texture nil
                    :shadow-grey-texture nil
                    :shadow-black-texture nil
                    :boot-texture nil
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
                    :change-to-text nil
                    :change-to-table nil
                    :change-to-decoration nil
                    :background-overlay nil
                    :current-stage nil})

(defonce state (r/atom initial-state))

(def stages
  [{:table-cycle ["none" "coins-small"]
    :win-con "coins-small"
    :cycle? false}
   {:table-cycle ["none" "coins-small" "coins-big"]
    :win-con "coins-big"
    :cycle? false}
   {:table-cycle ["none" "poop-small"]
    :win-con "poop-small"
    :cycle? true}])

(defn set-stage!
  [n]
  (let [table-cycle (r/cursor state [:table-cycle])
        win-con (r/cursor state [:win-con])
        cycle? (r/cursor state [:cycle?])
        current-stage (r/cursor state [:current-stage])
        stage (nth stages n)]
    (.log js/console "stage " (clj->js stage))
    (reset! current-stage n)
    (reset! table-cycle (:table-cycle stage))
    (reset! win-con (:win-con stage))
    (reset! cycle? (:cycle? stage))))

(defn next-stage
  []
  (let [current-stage (r/cursor state [:current-stage])
        next-stage (+ @current-stage 1)]
    (if (> next-stage (- (count stages) 1))
      0
      next-stage)))


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

(defn boot
  []
  (let [texture @(r/cursor state [:boot-texture])
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
  [font-atom text]
  (let [geometry (js/THREE.TextGeometry. text
                                         (clj->js {:font @font-atom
                                                   :size 50
                                                   :height 10}))
        material (js/THREE.MeshBasicMaterial. (clj->js {:color 0xD4AF37}))
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
        (let [;; x-center (/ (- ($ bounding-box :max.x)
              ;;                ($ bounding-box :min.x))
              ;;             2)
              ;; y-center (/
              ;;           (- ($ bounding-box :max.y)
              ;;              ($ bounding-box :min.y))
              ;;           2)
              ]
          ($! object3d :position.x x)
          ($! object3d :position.y y)
          (.updateBox this))))))

(defn set-tables
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
                           "poop-big" (poop-big-decoration))]
    (when (not (nil? table-decoration))
      (.moveTo table-decoration table-x (+ table-y 50))
      ($ @scene add (.getObject3d table-decoration)))
    table-decoration))

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
             (doall (map set-decoration! tables))))))

(defn stage-won?
  [tables win-con]
  (every? true? (map #(= (.getDecoration %) win-con) tables)))

(defn play-again-fn
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
    (reset! time-fn (play-again-fn))
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
    (reset! time-fn (play-again-fn))
    (r/render
     [GameLostScreen {:selected-menu-item selected-menu-item}]
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
        scene (r/cursor state [:scene])
        font-atom (r/cursor state [:font])]
    (reset! score new-score)
    ($ @scene remove (.getObject3d @score-text))
    (reset! score-text (text font-atom @score))
    (.moveTo @score-text -1200 700)
    ($ @scene add (.getObject3d @score-text))))

(defn init-stage
  [state]
  (let [scene (r/cursor state [:scene])
        hero (r/cursor state [:hero])
        broom (r/cursor state [:broom])
        boot (r/cursor state [:boot])
        shadow (r/cursor state [:shadow])
        font-atom (r/cursor state [:font])
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
        win-con (r/cursor state [:win-con])]
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
             ;;(+ -600 @broom-offset)
             10000
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
    ;; set the total score
    ;; do this later
    ;; make sure we aren't dead
    (reset! died? false)
    ;; show our lives total
    (show-lives! state)
    ;; show the current score
    (reset-score! state @score)
    ;; show the change to menu text
    ;; (reset! change-to-text (text font-atom "Tables be like"))
    ;; (.moveTo @change-to-text -1200 350)
    ;; ($ scene add (.getObject3d @change-to-text))
    ;; show the change to menu table
    (reset! change-to-table (table))
    (.moveTo @change-to-table -1000 200)
    ($ @scene add (.getObject3d @change-to-table))
    ;; set the decorations to win-con
    (.setDecoration @change-to-table @win-con)
    ;; show the change to decoration
    (reset! change-to-decoration (set-decoration! @change-to-table))
    ;; initialize table
    (doall (map #(.setDecoration % (first (:table-cycle @state))) @tables))
    (doall (map #(.log js/console (.getDecoration %)) @tables))
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
        p-ticks-counter (r/cursor state [:p-ticks-counter])
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
        move-hero! (fn [hero allowed-directions direction]
                     (when (direction allowed-directions)
                       ;; move hero
                       (.moveTo hero
                                ($ (.getObject3d (direction allowed-directions))
                                   :position.x)
                                (+  ($ (.getObject3d (direction allowed-directions)) :position.y)
                                    @hero-offset))
   ;;                    (.log js/console (count @table-decorations))
 ;;                      (doall (map #(.log js/console (.getDecoration %)) @tables))
                       ;; redraw the table decorations
                       (set-decorations! @tables @table-decorations state)
                       ;; reset the score
                       (reset-score! state (calculate-score state))
                       ;; redraw the score
                       ($ js/createjs Sound.play (:hop @state))))
        move-broom! (fn [broom table]
                      (.moveTo broom
                               ($ (.getObject3d table) :position.x)
                               (+  ($ (.getObject3d table) :position.y)
                                   @broom-offset)))
        table-cycle (r/cursor state [:table-cycle])
        cycle? (r/cursor state [:cycle?])
        win-con (r/cursor state [:win-con])
        lives (r/cursor state [:lives])
        died? (r/cursor state [:died?])
        current-stage (r/cursor state [:current-stage])]
    (fn [delta-t]
      (@render-fn)
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
      (set-decorations-cycle! @table-cycle @cycle? @tables)
      ;; is the stage won?
      (when (stage-won? @tables @win-con)
        (.log js/console "I think I won")
        (.log js/console "current-stage is: " @current-stage)
        ;; set to the next stage
        (set-stage! (next-stage))
        ;; reinitialize the stage
        ;;(init-game-won-screen)
        (init-stage state))
      ;; set the allowed directions
      (reset! hero-allowed-directions (allowed-directions (occupied-table @hero @tables) @tables))
      ;;(reset! broom-allowed-directions (allowed-directions (occupied-table @broom @tables) @tables))
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
        ;; (when (= @broom-ticks broom-max-ticks)
        ;;   (let [table-options (filter (comp not nil?) (vals @broom-allowed-directions))]
        ;;     (move-broom! @broom (nth table-options (rand-int (count table-options))))
        ;;     (reset! broom-ticks 0)))
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
        ;; (when (= @boot-ticks boot-ticks-max)
        ;;   (let [shadow-table (occupied-table @shadow @tables)]
        ;;     (if (not (nil? shadow-table))
        ;;       (let [table-x ($ (.getObject3d shadow-table) :position.x)
        ;;             table-y ($ (.getObject3d shadow-table) :position.y)]
        ;;         (.moveTo @boot table-x (+ table-y @boot-offset))
        ;;         (reset! boot-ticks 0)))))
        ;; is the game lost?
        (when (= (occupied-table @hero @tables)
                 (occupied-table @broom @tables))
          (when (= @lives 0)
            (reset! died? true)
            (init-game-lost-screen))
          (when (> @lives 0)
            (swap! lives dec)
            (show-lives! state)
            (reset! died? true)))
        (when (= (occupied-table @hero @tables)
                 (occupied-table @boot @tables))
          (when (= @lives 0)
            (reset! died? true)
            (init-game-lost-screen))
          (when (> @lives 0)
            (swap! lives dec)
            (show-lives! state)
            (reset! died? true))))
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
        hero (pigeon)
        broom (broom)
        boot (boot)
        font-atom (r/cursor state [:font])
        tables (set-tables (:stage @state))
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
        shadow (table-decoration @(r/cursor state [:shadow-black-texture]) 130 22)
        lives (r/cursor state [:lives])
        score (r/cursor state [:score])
        score-text (r/cursor state [:score-text])
        change-to-text (r/cursor state [:change-to-text])
        change-to-table (r/cursor state [:change-to-table])
        change-to-decoration (r/cursor state [:change-to-decoration])
        win-con (r/cursor state [:win-con])
        table-cycle (r/cursor state [:table-cycle])
        win-con (r/cursor state [:win-con])
        cycle? (r/cursor state [:cycle?])]
    (swap! state assoc
           :render-fn render-fn
           :hero hero
           :broom broom
           :boot boot
           :tables tables
           :shadow shadow
           :scene scene
           :camera camera)
    (.updateBox hero)
    (.updateBox broom)
    (.updateBox boot)
    ($ scene add (.getObject3d hero))
;;;    ($ scene add (.getBoxHelper hero))
    ($ scene add (.getObject3d broom))
    ($ scene add (.getObject3d boot))
    ($ scene add (.getObject3d shadow))
    ;; ($! (.getObject3d shadow) :position.z 9)
    ($ scene add (origin))
    (reset! lives 0)
    ;;(.moveTo hero 0 690)
    ;; (.moveTo hero 0 ;;700
    ;;          700
    ;;          )
    ;; (.moveTo broom
    ;;          1200 ;;-480
    ;;          (+ -600 @broom-offset))
    ;; (.moveTo boot
    ;;          0
    ;;          10000)
    ;; (.moveTo shadow 0
    ;;          (+ 600 @(r/cursor state [:shadow-offset])))
    ;; (reset! broom-ticks 0)
    ;; (reset! shadow-ticks 0)
    ;; (reset! boot-ticks 0)
    ;; (reset! died-ticks 0)
    ;;    (reset! score (calculate-score state))
    ;; score is initiall 0
    (reset! score 0)
    ;;    (reset! died? false)
    ;; add the tables to the scene
    (doall (map (fn [table]
                  ($ scene add (.getObject3d table))) tables))
    ;; show lives total
    (show-lives! state)
    ;; set the score text
    (reset! score-text (text font-atom @score))
    ;; (.moveTo @score-text -1200 700)
    ;; ($ scene add (.getObject3d @score-text))
    ;; show the change to menu text
    (reset! change-to-text (text font-atom "Change To"))
    (.moveTo @change-to-text -1190 350)
    ($ scene add (.getObject3d @change-to-text))
    ;; show the change to menu table
    ;; (reset! change-to-table (table))
    ;; (.moveTo @change-to-table -1000 200)
    ;; ($ scene add (.getObject3d @change-to-table))
    ;; set the decoration to win-con
    ;;(.setDecoration @change-to-table @win-con)
    ;; show the change to decoration
    ;; (reset! change-to-decoration (set-decoration! @change-to-table))
    ;; initial table decorations
    ;; (doall (map #(.setDecoration % (first (:table-cycle @state))) tables))
    ;; (set-decorations! tables @table-decorations state)
    (reset! time-fn (game-fn))
    ;; set the proper stage
    (set-stage! 0)
    (init-stage state)
    (display/window-resize! renderer camera)
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
        poop-big-texture (r/cursor state [:poop-big-texture])
        boot-texture (r/cursor state [:boot-texture])
        ;;        shadow-grey-texture (r/cursor state [:shadow-grey-texture])
        shadow-black-texture (r/cursor state [:shadow-black-texture])]
    (reset! table-texture (js/THREE.ImageUtils.loadTexture. "images/table_red.png"))
    (reset! hero-texture (js/THREE.ImageUtils.loadTexture. "images/pigeon_right_a.png"))
    (reset! broom-texture (js/THREE.ImageUtils.loadTexture. "images/broom.png"))
    (reset! coins-small-texture (js/THREE.ImageUtils.loadTexture. "images/coins_small.png"))
    (reset! coins-big-texture (js/THREE.ImageUtils.loadTexture. "images/coins_big.png"))
    (reset! poop-small-texture (js/THREE.ImageUtils.loadTexture. "images/poop_small.png"))
    (reset! poop-medium-texture (js/THREE.ImageUtils.loadTexture. "images/poop_medium.png"))
    (reset! poop-big-texture (js/THREE.ImageUtils.loadTexture. "images/poop_big.png"))
    ;;    (reset! shadow-grey-texture (js/THREE.ImageUtils.loadTexture. "images/shadow_grey.png"))
    (reset! shadow-black-texture (js/THREE.ImageUtils.loadTexture. "images/shadow_black.png"))
    (reset! boot-texture (js/THREE.ImageUtils.loadTexture. "images/boot_a.png"))
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
        init-play-again-fn (r/cursor state [:init-play-again-fn])
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
