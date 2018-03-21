(ns com.sixsq.slipstream.db.serializers.service-config-impl
  (:require
    [clojure.edn :as edn]
    [clojure.set :as set]
    [clojure.tools.logging :as log]

    [camel-snake-kebab.core :refer [->camelCase]]
    [superstring.core :as s]

    [com.sixsq.slipstream.db.serializers.utils :as u]
    [com.sixsq.slipstream.db.serializers.service-config-util :as scu]
    [com.sixsq.slipstream.ssclj.resources.configuration :as cr]
    [com.sixsq.slipstream.ssclj.resources.configuration-slipstream :as crs]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as cts]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as crtpl]
    [com.sixsq.slipstream.ssclj.resources.connector :as con]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as cont])
  (:import
    (com.sixsq.slipstream.persistence ServiceConfiguration)))


(def connector-ref-attrs-defaults (merge cont/connector-mandatory-reference-attrs-defaults
                                         cont/connector-reference-attrs-defaults))

(def ^:const cfg-resource-uuid crs/service)

(def ^:const global-categories #{"SlipStream_Advanced" "SlipStream_Support" "SlipStream_Basics"})

(def user-roles-str "me ADMIN")

(def user-roles ((juxt first rest) (s/split user-roles-str #"\s+")))


(def rname->param
  {:serviceURL                   "slipstream.base.url"
   :supportEmail                 "slipstream.support.email"
   :clientBootstrapURL           "slipstream.update.clientbootstrapurl"
   :clientURL                    "slipstream.update.clienturl"
   :connectorOrchPrivateSSHKey   "cloud.connector.orchestrator.privatesshkey"
   :connectorOrchPublicSSHKey    "cloud.connector.orchestrator.publicsshkey"
   :connectorLibcloudURL         "cloud.connector.library.libcloud.url"

   :mailUsername                 "slipstream.mail.username"
   :mailPassword                 "slipstream.mail.password"
   :mailHost                     "slipstream.mail.host"
   :mailPort                     "slipstream.mail.port"
   :mailSSL                      "slipstream.mail.ssl"
   :mailDebug                    "slipstream.mail.debug"

   :quotaEnable                  "slipstream.quota.enable"

   :registrationEnable           "slipstream.registration.enable"
   :registrationEmail            "slipstream.registration.email"

   :meteringEnable               "slipstream.metering.enable"
   :meteringEndpoint             "slipstream.metering.hostname"

   :serviceCatalogEnable         "slipstream.service.catalog.enable"

   :slipstreamVersion            "slipstream.version"

   :cloudConnectorClass          "cloud.connector.class"

   :metricsLoggerEnable          "slipstream.metrics.logger.enable"
   :metricsGraphiteEnable        "slipstream.metrics.graphite.enable"

   :reportsObjectStoreCreds      "slipstream.reports.objectstore.creds"
   :reportsObjectStoreBucketName "slipstream.reports.objectstore.bucket.name"})

(def param->rname (set/map-invert rname->param))

(def connector-ref-attrs-kw->pname
  {:orchestratorImageid "orchestrator.imageid"
   :quotaVm             "quota.vm"
   :maxIaasWorkers      "max.iaas.workers"})

(def connector-mandatory-atrrs-kw->pname
  {:endpoint                "endpoint"
   :nativeContextualization "native-contextualization"
   :orchestratorSSHUsername "orchestrator.ssh.username"
   :orchestratorSSHPassword "orchestrator.ssh.password"
   :securityGroups          "security.groups"
   :updateClientURL         "update.clienturl"})

(def connector-kw->pname
  (merge connector-ref-attrs-kw->pname
         connector-mandatory-atrrs-kw->pname))

(def connector-pname->kw (set/map-invert connector-kw->pname))

(defn global-param-key
  [p]
  (get param->rname (.getName p)))

(defn category-global?
  [p]
  (contains? global-categories (.getCategory p)))

(defn param-global-valid?
  [p]
  (and (category-global? p) (global-param-key p)))

(defn conn-pname-to-kwname
  [cn pname]
  (-> (merge connector-pname->kw
             (get @cont/name->kw (str cont/resource-url "/" cn)))
      (get pname (->camelCase pname :separator #"\.|-"))))

(defn connector-param-name-as-kw
  [p cin->cn]
  (let [[cin pname] (u/param-get-cat-and-name p)
        cn (get cin->cn cin)]
    (if (s/blank? pname)
      (log/warn "Parameter name is blank when mapping connector parameter for:" cin)
      (->> pname
           (conn-pname-to-kwname cn)
           keyword))))

(defn process-param-value
  [v]
  (cond
    (nil? v) ""
    (and (= (type v) java.lang.String) (empty? v)) ""
    (= (type (u/read-str v)) clojure.lang.Symbol) v
    :else (u/read-str v)))

(defn param-value
  [p]
  (let [v (.getValue p)]
    (cond
      (= "boolean" (s/lower-case (.getType p))) (process-param-value v)
      (integer? (process-param-value v)) (read-string v)
      :else v)))

(defn conn-param-value-type-from-template
  [p cn]
  (let [resource-name (str cont/resource-url "/" cn)
        template (get @cont/templates resource-name)
        cnkw (keyword (conn-pname-to-kwname cn (u/param-get-pname p)))
        val (cnkw template)]
    (type val)))

(defn conn-param-value
  [p cn]
  (let [v (.getValue p)
        templ-val-type (conn-param-value-type-from-template p cn)]
    (if (= java.lang.String templ-val-type)
      (str v)
      (param-value p))))

(defn cfg->sc
  ([cfg]
   (cfg->sc cfg {}))
  ([cfg cfg-desc]
   (let [sc (ServiceConfiguration.)]
     (doseq [pk (keys cfg)]
       (if (contains? rname->param pk)
         (let [value (pk cfg)
               desc (assoc (or (pk cfg-desc) {}) :name (pk rname->param))]
           (.setParameter sc (u/build-sc-param value desc)))))
     sc)))

(defn param-for-sc->cfg?
  [p]
  (param-global-valid? p))

(defn assoc-cfg-identity
  [cfg]
  (assoc cfg :id (str cr/resource-url "/" crs/service)))

(defn sc->cfg
  [sc]
  (-> {}
      (into (for [p (vals (.getParameters sc)) :when (param-for-sc->cfg? p)]
              [(global-param-key p) (param-value p)]))
      assoc-cfg-identity))

(defn sc->cfg-desc
  [sc]
  (into {}
        (for [p (vals (.getParameters sc)) :when (param-global-valid? p)]
          [(global-param-key p) (u/desc-from-param p)])))

;; In ServiceConfiguration, category is either one of global-categories or
;; connector instance name.

(defn non-gobal-category-match?
  [param category]
  (and (not (param-global-valid? param)) (= category (.getCategory param))))

(defn non-global-categories
  [sc]
  (into #{}
        (for [p (vals (.getParameters sc)) :when (not (category-global? p))]
          (.getCategory p))))

(defn grow-coll-1->2
  [c]
  (if (= 1 (count c))
    (conj c (first c))
    c))

(defn connector-names-map
  "Input (str): instance1:connector1,connector2,...
  Output ([cin cn] ..): connector instance name -> connector name map. Keys and values are strings.
  Cases:
  1. instance-name:name
  2. name -> name:name (duplicate name)
  "
  [cnames]
  (into {}
        (if (seq cnames)
          (->> (s/split cnames #",")
               (map #(s/trim %))
               (filter #(seq %))
               (map #(s/split % #":"))
               (map grow-coll-1->2)))))

(defn sc-connector-names-map
  [sc]
  (-> sc
      (scu/sc-get-param-value "cloud.connector.class")
      connector-names-map))

(defn conn-name
  "Given parameter and connector instance name to name mapping,
  return the parameter's connetor name."
  [p cin->cn]
  (let [cin (first (s/split (.getName p) #"\."))]
    (get cin->cn cin)))

(defn assoc-conn-identity
  [conn sc cin]
  (assoc conn :id (str con/resource-url "/" cin)
              :cloudServiceType (get (sc-connector-names-map sc) cin)))

(defn param-non-global-and-named?
  [p cin cin->cn]
  (if (and (non-gobal-category-match? p cin) (connector-param-name-as-kw p cin->cn))
    true
    false))

(defn sc->connector
  "Parameter name is a string with removed category, i.e, [cat.]param.name.
  Mapping to keywords is looked up in local map and in the connector one
  registerred in connector-template/name->kw atom."
  [sc cin]
  (let [cin->cn (sc-connector-names-map sc)]
    (-> {}
        (into (for [p (vals (.getParameters sc)) :when (param-non-global-and-named? p cin cin->cn)]
                (let [kw (connector-param-name-as-kw p cin->cn)]
                  [kw (conn-param-value p (conn-name p cin->cn))])))
        (assoc-conn-identity sc cin))))

(defn sc->connector-desc
  [sc cin]
  (let [cin->cn (sc-connector-names-map sc)]
    (into {}
          (for [p (vals (.getParameters sc)) :when (param-non-global-and-named? p cin cin->cn)]
            [(connector-param-name-as-kw p cin->cn)
             (u/desc-from-param p)]))))

(defn sc->connector-with-desc
  [sc cin]
  [(sc->connector sc cin)
   (sc->connector-desc sc cin)])

(defn sc->connectors-base
  [sc cins func]
  (let [cins (if (seq cins)
               cins
               (non-global-categories sc))]
    (into {}
          (for [cin cins]
            [(keyword cin) (func sc cin)]))))

(defn sc->connectors
  "Given ServiceConfiguration, returns the following map
  {:connector-instance-name [{:param val
                              ..}
                             {:param {description map}
                              ..}]
   ..}

   cin - connector instance name.
   "
  [sc & [cins]]
  (sc->connectors-base sc cins sc->connector-with-desc))

(defn sc->connectors-vals-only
  "Given ServiceConfiguration, returns the following map
  {:connector-instance-name {:param1 val
                             :param2 val
                             ..}
   ..}

   cin - connector instance name.
   "
  [sc & [cins]]
  (sc->connectors-base sc cins sc->connector))

(defn cs->cfg-desc-and-spit
  [sc fpath]
  (scu/spit-pprint (sc->cfg-desc sc) fpath))

(defn cs->cfg-and-spit
  [sc fpath]
  (scu/spit-pprint (sc->cfg sc) fpath))

(def cfg-desc cts/desc)

(defn connector-template-desc
  []
  cont/ConnectorTemplateDescription)

(defn complete-resource
  [cfg]
  (-> cfg
      (assoc :service crs/service)
      crtpl/complete-resource))

(defn resource-as-request
  [name & [body]]
  (u/as-request (or body {}) name user-roles-str))

(defn cfg-as-request
  [& [body]]
  (resource-as-request cfg-resource-uuid body))

(defn configuration-as-request
  [name & [body]]
  (resource-as-request name body))

(defn connector-as-request
  [cin & [body]]
  (resource-as-request cin body))


(defn get-sc-param-meta-only
  [pname]
  (u/build-sc-param "" ((get param->rname pname) cfg-desc)))

(defn get-connector-param-from-template
  [pname]
  (let [kw (get connector-pname->kw pname)
        value (kw cont/connector-mandatory-reference-attrs-defaults)]
    (u/build-sc-param value (kw (connector-template-desc)))))

(def unwanted-attrs #{:id :resourceURI :acl :operations
                      :created :updated :name :description
                      :cloudServiceType
                      :instanceName})

(defn strip-unwanted-attrs
  [m & [unwanted]]
  (into {} (remove #((or unwanted unwanted-attrs) (first %)) m)))

(defn get-connector-params-from-template
  [con-name]
  (let [resource-name (str cont/resource-url "/" con-name)
        connector-template (->> resource-name (get @cont/templates) strip-unwanted-attrs)
        connector-desc (get @cont/descriptions resource-name)]
    (for [[k value] connector-template]
      (u/build-sc-param value (k connector-desc)))))


;;
;; Configuration and connector resource getters.
;;


(defn resp-success?
  [resp]
  (< (:status resp) 400))

(defn get-document
  [request retrieve-impl]
  (let [resp (retrieve-impl request)]
    (if (resp-success? resp)
      (let [unwanted (into #{} (remove #{:id :cloudServiceType :description} unwanted-attrs))]
        (strip-unwanted-attrs (:body resp) unwanted))
      (do
        (u/warn-on-resp-error resp)
        nil))))

(defn get-connector
  [name]
  (get-document (connector-as-request name) con/retrieve-impl))

(defn get-configuration
  [name]
  (get-document (configuration-as-request name) cr/retrieve-impl))

;;
;;  Delete wrappers for configuration and connectors.
;;

(defn delete-connector
  [name]
  (con/delete-impl (connector-as-request name)))

(defn delete-config
  [name]
  (cr/delete-impl (configuration-as-request name)))

;;
;; Utility wrappers around configuration CRUD.
;;

(defn- check-response
  "{response}, operation name, fail with exception (false/true)"
  [resp op fail]
  (when (> (:status resp) 400)
    (if fail
      (u/throw-on-resp-error resp)
      (log/warn "Failure" op "configuration:" resp))))

(defn db-edit-config-from-file
  [f & [fail]]
  (log/info "Editing server configuration in ES DB from file:" f)
  (let [c (edn/read-string (slurp f))
        resp (-> (get-configuration "slipstream")
                 complete-resource
                 (merge c)
                 (u/as-request cfg-resource-uuid user-roles-str)
                 cr/edit-impl)]
    (check-response resp "editing" fail)))

(defn db-add-default-config
  [& [fail]]
  (log/info "Adding default server configuration to ES DB.")
  (let [resp (-> cts/resource
                 complete-resource
                 (u/as-request cfg-resource-uuid user-roles-str)
                 cr/add-impl)]
    (check-response resp "adding" fail)))

;;
;; Store and load of configuration and connectors.
;;

(defn store-sc
  [^ServiceConfiguration sc]
  (-> sc
      sc->cfg
      cfg-as-request
      cr/edit-impl
      u/throw-on-resp-error)
  sc)

(defn store-connectors
  [^ServiceConfiguration sc]
  (let [cin-cn (sc-connector-names-map sc)
        connectors-vals (sc->connectors-vals-only sc (keys cin-cn))
        ]
    (doseq [[cnamekw cvals] connectors-vals]
      (let [cname (name cnamekw)
            req (connector-as-request cname (merge cvals {:instanceName     cname
                                                          :cloudServiceType (get cin-cn cname)}))
            _ (log/info "Editing connector instance:" cname)
            edit-resp (con/edit-impl req)
            edit-status (:status edit-resp)
            _ (log/info "Edit response status:" edit-status)]
        ;; editing may fail because connector instance wasn't instantiated yet.
        (if (= 404 edit-status)
          (do
            (try
              (log/info "Adding for the first time connector:" cname)
              ;; adding may fail because connector template wasn't registered yet.
              (-> req
                  con/add-impl
                  u/warn-on-resp-error)
              (log/info "Successfully added data for connector:" cname)
              (catch Exception e
                (log/warn "Failed to edit or add data for connector" cname ". Caught:" (.getMessage e)))))
          (u/warn-on-resp-error edit-resp)))))
  sc)

(defn load-cfg
  []
  (-> (cfg-as-request)
      cr/retrieve-impl
      :body))

(defn load-sc
  []
  (cfg->sc (load-cfg) cfg-desc))

(defn load-connectors
  "Loads only the connectors defined in cloud.connector.class.
  This is done for the compatibility with the current logic of the service."
  [^ServiceConfiguration sc]
  (let [cin-cn (sc-connector-names-map sc)]
    (log/info "Loading connectors" cin-cn)
    (doseq [[cin cn] cin-cn]
      (log/info "Loading connector" cin cn)
      (let [values (strip-unwanted-attrs (:body (con/retrieve-impl (connector-as-request cin))))
            descs (get @cont/descriptions (str cont/resource-url "/" cn))]
        (doseq [[vk vv] values]
          (log/debug "Setting ServiceConigurationParameter on ServiceConfiguration" vk vv)
          (.setParameter sc (u/build-sc-param vv (vk descs) cin))))))
  sc)
