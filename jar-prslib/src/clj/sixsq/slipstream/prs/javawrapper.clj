(ns sixsq.slipstream.prs.javawrapper
  "
  # Java interface to library for interaction with Placement and Ranking Service.
  "
  {:doc/format :markdown}
  (:require
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [clojure.data.json :as json])
  (:import [java.util Map List Set]
           [com.sixsq.slipstream.persistence Module ImageModule ModuleCategory ModuleCategory]
           [com.sixsq.slipstream.configuration Configuration]
           )
  (:gen-class
    :name sixsq.slipstream.prs.core.JavaWrapper
    :methods [#^{:static true} [generatePrsRequest [java.util.Map] String]]))

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
  {:module                   (.getResourceUri comp)
   :cpu.nb                   (parameter-value comp "cpu.nb")
   :ram.GB                   (parameter-value comp "ram.GB")
   :disk.GB                  (parameter-value comp "disk.GB")
   :operating-system         (if (= "windows" (.getPlatform comp)) "windows" "linux")
   :placement-policy         (.getPlacementPolicy comp)
   :connector-instance-types (apply merge (map (partial connector-instance-types comp) user-connectors))})

(defn- connector->orchestrator-map
  [connector]
  (let [orchestrator-instance-type (-> (Configuration/getInstance)
                                       (.getProperty (format "%s.orchestrator.instance.type" connector)))
        type-policy                (if orchestrator-instance-type (format " and schema-org:name='%s'" orchestrator-instance-type) "")]
    {:node             (str "node-orchestrator-" connector)
     :module           (str "module-orchestrator-" connector)
     :cpu.nb           "0"
     :ram.GB           "0"
     :disk.GB          "0"
     :operating-system "linux"
     :placement-policy (format "connector/href='%s'%s" connector type-policy)}))

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
    (app? module) (app->map module user-connectors)
    :else (throw-wrong-category module)))

(defn- add-orchestrator-components
  [m user-connectors]
  (update m :components #(concat % (map connector->orchestrator-map user-connectors))))

(defn- explode-module
  [m]
  (-> m
      (assoc :components (module->components (:module m) (:user-connectors m)))
      (add-orchestrator-components (:user-connectors m))
      (dissoc :orchestratorComponents)
      (dissoc :module)))

(defn placement->map
  "Converts java-placement (Java Map) to Clojure data structure.
  The module object is extracted as a list of components."
  [java-placement]
  (-> java-placement
      java->clj
      walk/keywordize-keys
      explode-module))

(defn -generatePrsRequest
  "Generate PRS request based on module and users connector information.
  Input
  {
    :module           Module        // java object - ImageModule or DeploymentModule
    :user-connectors  ['c1' 'c2']   // list of strings
   }
  Output: valid JSON to contact PRS service.
  "
  [input]
  (log/debug "Generating PRS request for:" input)
  (-> input
      placement->map
      json/write-str))


