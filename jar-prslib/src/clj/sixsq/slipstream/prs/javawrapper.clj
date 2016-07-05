(ns sixsq.slipstream.prs.javawrapper
  "
  # Java interface to library for interaction with Placement and Ranking Service.
  "
  {:doc/format :markdown}
  (:require
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [sixsq.slipstream.prs.core :as prs]
    )
  (:import [java.util Map List Set]
           [com.sixsq.slipstream.persistence ImageModule DeploymentModule ModuleCategory NodeParameter ModuleCategory])
  (:gen-class
    :name sixsq.slipstream.prs.core.JavaWrapper
    :methods [#^{:static true} [placeAndRank      [java.util.Map] String]
              #^{:static true} [validatePlacement [com.sixsq.slipstream.persistence.Run] Boolean]]))

(defn java->clj
  "Transform java data structures into the equivalent clojure
   data structures.  Elements that are not a Map, List, or Set
   are left unchanged."
  [data]
  (cond
    (instance? Map data) (into {} (map (fn [[k v]] [k (java->clj v)]) data))
    (instance? List data) (into [] (map #(java->clj %) data))
    (instance? Set data) (into #{} (map #(java->clj %) data))
    :else data))

(defn comp->map
  [comp]
  {:module           (.getResourceUri comp)
   :vm-size          "undefined"
   :placement-policy "undefined"})

(defn- node->map
  [[node comp]]
  {:module            (-> comp .getImage .getResourceUri)
   :node              node
   :vm-size           "undefined"
   :placement-policy  "undefined"})

(defn app->map
  "Takes app as DeploymentModule and returns a list of components."
  [app]
  (map node->map (.getNodes app)))

(defn components-from-module
  [module]
  (let [category (.getCategory module)]
    (cond
      (= ModuleCategory/Image category) [(comp->map module)]
      (= ModuleCategory/Deployment category) (app->map module)
      :else (Exception. (format "Expected module of category %s or %s."
                                ModuleCategory/Image ModuleCategory/Deployment)))))

(defn module-to-map
  [module]
  {:components (components-from-module module)})

(defn process-module
  [m]
  (update m :module module-to-map))

(defn -placeAndRank
  "Given the plament request as map returns JSON response from PRS service.

  Placement request map
  {
    module: Module, // java object - ImageModule or DeploymentModule
    placement-params: { components: [ ] }, // java map
    prs-endpoint: url, // string
    user-connectors: ['c1' ] // list of strings
   }
  "
  [input]
  (-> (java->clj input)
      (walk/keywordize-keys)
      (process-module)
      (prs/place-and-rank)))

(defn -validatePlacement
  [run]
  (log/info "Calling PRS validater")
  ;; run
  ;; -> placementRequest
  ;; (placeAndRank)
  ; Output
  ; {:components [{:module uri
  ;               :connectors [{:name c1 :price 0 :currency ''},
  ;                             {:name c2 :price 0 :currency ''}]}]
  ; }
  ;; valid means : for each module, in returned components, connectors not empty
  true)

