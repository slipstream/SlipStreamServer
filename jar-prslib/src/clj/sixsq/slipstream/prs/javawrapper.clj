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
           [com.sixsq.slipstream.persistence Module ImageModule ModuleCategory ModuleCategory])
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

(defn- parameter-value
  [comp param-name]
  (.. comp (getParameter param-name) (getValue)))

(defn- comp->map
  [comp]
  {:module           (.getResourceUri comp)
   :cpu.nb           (parameter-value comp "cpu.nb")
   :ram.GB           (parameter-value comp "ram.GB")
   :disk.GB          (parameter-value comp "disk.GB")
   :placement-policy (.getPlacementPolicy comp)})

(defn- node->map
  [[node-name node]]
  (-> (.getImage node)
      comp->map
      (assoc :node node-name)))

(defn- app->map
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

(defn- module->components
  [module]
  (cond
    (component? module) [(comp->map module)]
    (app? module)       (app->map module)
    :else               (throw-wrong-category module)))

(defn- explode-module
  [m]
  (-> m
      (assoc :components (module->components (:module m)))
      (dissoc :module)))

(defn placement->map
  "Converts java-placement (Java Map) to Clojure data structure.
  The module object is extracted as a list of components."
  [java-placement]
  (let [result (-> java-placement
                   java->clj
                   walk/keywordize-keys
                   explode-module)]
    (log/info "placement->map : " result)
    result))

(defn -placeAndRank
  "Input is translated into map, PRS service is called and returns a JSON response.
  Input
  {
    :module           Module        // java object - ImageModule or DeploymentModule
    :prs-endpoint     url           // string
    :user-connectors  ['c1' 'c2']   // list of strings
   }
  "
  [input]
  (log/info "javawrapper, input for place-and-rank" input)
  (-> input
      placement->map
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

