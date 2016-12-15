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
  (if-let [param (.. comp (getParameter param-name))]
    (.getValue param)))

(defn- connector-instance-types
  [comp user-connector]
  {user-connector (parameter-value comp (str user-connector ".instance.type"))})

(defn- comp->map
  [comp user-connectors]
  {:module                    (.getResourceUri comp)
   :cpu.nb                    (parameter-value comp "cpu.nb")
   :ram.GB                    (parameter-value comp "ram.GB")
   :disk.GB                   (parameter-value comp "disk.GB")
   :placement-policy          (.getPlacementPolicy comp)
   :connector-instance-types  (apply merge (map (partial connector-instance-types comp) user-connectors))})

(defn- node->map
  [user-connectors [node-name node]]
  (-> (.getImage node)
      (comp->map user-connectors)
      (assoc :node node-name)))

(defn- app->map
  [app user-connectors]
  (map (partial node->map user-connectors) (.getNodes app)))

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
  [module user-connectors]
  (cond
    (component? module) [(comp->map module user-connectors)]
    (app? module)       (app->map module user-connectors)
    :else               (throw-wrong-category module)))

(defn- add-orchestrator-component
  [m scalable?]
  (if scalable?
    (update m :components
               #(conj % {:module                   "orchestrator"
                         :node                     "orchestrator"
                         :placement-policy         nil
                         :connector-instance-types {}}))
    m))

(defn- explode-module
  [m]

  (log/info "explode module, scalable ? " (:isScalable m))

  (-> m
      (assoc :components (module->components (:module m) (:user-connectors m)))
      (add-orchestrator-component (:isScalable m))
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

