(ns sixsq.slipstream.prs.javawrapper
  "
  # Java interface to library for interaction with Placement and Ranking Service.
  "
  {:doc/format :markdown}
  (:require
    [clojure.walk :as walk]
    [sixsq.slipstream.prs.core :as prs])
  (:import [java.util Map List Set]
           [com.sixsq.slipstream.persistence ImageModule DeploymentModule ModuleCategory])
  (:gen-class
    :name sixsq.slipstream.prs.core.JavaWrapper
    :methods [#^{:static true} [placeAndRank [java.util.Map] String]]))

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

(defn comp-to-map
  [comp]
  {:uri              (.getResourceUri comp)
   :vm-size          ""
   :placement-policy ""})

(defn comps-from-app
  "Takes app as DeploymentModule and returns a vector of components
  {:comp-uri uri :multiplicity 0 :placement-policy ''}
  "
  [app]
  (let [comps []]
    (for [node (.getNodes app)
          :let [comp (.getImage node)]]
      (into comps (comp-to-map comp)))
    comps))

(defn components-from-module
  [module]
  (let [category (.getCategory module)]
    (cond
      (= ModuleCategory/Image category) [(comp-to-map module)]
      (= ModuleCategory/Deployment category) (comps-from-app module)
      :else (Exception. (format "Expected module of category %s or %s."
                                ModuleCategory/Image ModuleCategory/Deployment)))))

(defn module-to-map
  [module]
  {:uri        (.getResourceUri module)
   :components (components-from-module module)})

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
