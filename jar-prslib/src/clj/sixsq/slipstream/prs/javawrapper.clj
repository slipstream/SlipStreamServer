(ns sixsq.slipstream.prs.javawrapper
  "
  # Java interface to library for interaction with Placement and Ranking Service.
  "
  {:doc/format :markdown}
  (:require
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [sixsq.slipstream.prs.core :as prs])
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
   :vm-size          "unused"
   :placement-policy "undefined"})

(defn- node->map
  [[node-name node]]
  {:module            (-> node .getImage .getResourceUri)
   :node              node-name
   :vm-size           "unused"
   :placement-policy  "undefined"})

(defn app->map
  "Takes app as DeploymentModule and returns a list of components."
  [app]
  (map node->map (.getNodes app)))

(defn- component?
  [module]
  (= (.getCategory module) ModuleCategory/Image))

(defn- app?
  [module]
  (= (.getCategory module) ModuleCategory/Deployment))

(defn- throw-wrong-category
  [module]
  (throw (Exception. (format "Expected module of category %s or %s. Got %s"
                             ModuleCategory/Image
                             ModuleCategory/Deployment
                             (.getCategory module)))))

(defn module->components
  [module]
  (cond
    (component? module) [(comp->map module)]
    (app? module)       (app->map module)
    :else               (throw-wrong-category module)))

(defn module->map
  [module]
  {:components (module->components module)})

(defn process-module
  [m]
  (update m :module module->map))

(defn- vm-size
  [user-connector comp]
  (if-let [parameter (.getParameter comp (str user-connector "." ImageModule/INSTANCE_TYPE_KEY))]
    [(.getResourceUri comp) (.getValue parameter)]
    [(.getResourceUri comp) "no mv size"]))

(defn- comp-or-app->components
  [module]
    (cond
      (component? module)  [module]
      (app? module)        (map (fn [[node-name node]] (.getImage node)) (.getNodes module))
      :else                (throw-wrong-category module)))

(defn- process-user-connector
  [m user-connector]
  (let [components (comp-or-app->components (:module m))]
    {:user-connector  user-connector
     :vm-sizes        (into {} (map (partial vm-size user-connector) components))}))

(defn- process-user-connectors
  [m]
  (update m :user-connectors #(map (partial process-user-connector m) %)))

(defn -placeAndRank
  "Input is translated into map, PRS service is called and returns a JSON response.

  Input
  {
    module: Module, // java object - ImageModule or DeploymentModule
    placement-params: { components: [ ] }, // java map
    prs-endpoint: url, // string
    user-connectors: ['c1' ] // list of strings
   }
  "
  [input]
  (log/info "Calling placeAndRank")
  (-> input
      java->clj
      walk/keywordize-keys
      process-user-connectors
      process-module
      prs/place-and-rank))

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

