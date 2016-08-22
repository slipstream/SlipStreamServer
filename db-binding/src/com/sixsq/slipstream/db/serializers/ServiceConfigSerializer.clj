(ns com.sixsq.slipstream.db.serializers.ServiceConfigSerializer
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.set :as set]
    [superstring.core :as s]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.impl :as db])
  (:import
    [com.google.gson GsonBuilder]
    [com.sixsq.slipstream.persistence ServiceConfiguration])
  (:gen-class
    :methods [#^{:static true} [store [com.sixsq.slipstream.persistence.ServiceConfiguration] com.sixsq.slipstream.persistence.ServiceConfiguration]
              #^{:static true} [load [] com.sixsq.slipstream.persistence.ServiceConfiguration]]))

(def acl
  {:owner {:principal "ADMIN"
           :type      "ROLE"}
   :rules [{:principal "ADMIN"
            :type      "ROLE"
            :right     "VIEW"}]}
  )

(def global-categories #{"SlipStream_Advanced" "SlipStream_Support" "SlipStream_Basics"})

(def rname->param
  {
   ; :name
   ; :description
   :serviceURL                 "slipstream.base.url"
   :reportsLocation            "slipstream.reports.location"
   :supportEmail               "slipstream.support.email"
   :clientBootstrapURL         "slipstream.update.clientbootstrapurl"
   :clientURL                  "slipstream.update.clienturl"
   :connectorOrchPrivateSSHKey "cloud.connector.orchestrator.privatesshkey"
   :connectorOrchPublicSSHKey  "cloud.connector.orchestrator.publicsshkey"
   :connectorLibcloudURL       "cloud.connector.library.libcloud.url"

   :mailUsername               "slipstream.mail.username"
   :mailPassword               "slipstream.mail.password"
   :mailHost                   "slipstream.mail.host"
   :mailPort                   "slipstream.mail.port"
   :mailSSL                    "slipstream.mail.ssl"
   :mailDebug                  "slipstream.mail.debug"

   :quotaEnable                "slipstream.quota.enable"

   :registrationEnable         "slipstream.registration.enable"
   :registrationEmail          "slipstream.registration.email"

   :prsEnable                  "slipstream.prs.enable"
   :prsEndpoint                "slipstream.prs.endpoint"

   :meteringEnable             "slipstream.metering.enable"
   :meteringEndpoint           "slipstream.metering.hostname"

   :serviceCatalogEnable       "slipstream.service.catalog.enable"
   })

(def param->rname (set/map-invert rname->param))

(defn read-str
  [s]
  (try
    (read-string s)
    (catch RuntimeException ex
      (if-not (s/starts-with? (.getMessage ex) "Invalid token")
        (throw ex)
        s))))

(defn param-value
  [p]
  (let [v (.getValue p)]
    (if (= (type (read-str v)) clojure.lang.Symbol)
      v
      (read-str v))))

(defn param-name
  [p]
  (get param->rname (.getName p)))

(defn param-global-valid?
  [p]
  (and (contains? global-categories (.getCategory p)) (param-name p)))

(defn sc->configuration-resource
  [sc]
  (into {}
        (for [p (vals (.getParameters sc))]
          (when (param-global-valid? p)
            [(param-name p) (param-value p)]))))

(defn -store
  [^ServiceConfiguration sc]
  (let [id (.getId sc)
        _ (println "ID .... " id)
        gson (-> (GsonBuilder.)
                 (.setPrettyPrinting)
                 (.enableComplexMapKeySerialization)
                 (.excludeFieldsWithoutExposeAnnotation)
                 (.create))
        doc (.toJson gson sc)
        data (esb/doc->data doc)]
    (println doc)
    (pprint data)
    (db/add "configuration" (assoc data :id "configuration") acl)
    (println (esu/dump esb/*client* esb/index-name "configuration"))
    )
  sc)

(defn -load
  []
  (pprint (db/retrieve "configuration" {:user-name "konstan" :user-roles ["ADMIN"]}))
  (ServiceConfiguration.))
