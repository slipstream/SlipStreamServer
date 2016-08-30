(ns com.sixsq.slipstream.db.serializers.service-config
  (:require
    [clojure.set :as set]
    [superstring.core :as s]
    [com.sixsq.slipstream.db.serializers.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as cr]
    [com.sixsq.slipstream.ssclj.resources.configuration-slipstream :as crs]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as cts]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as crtpl]
    )
  (:import
    [com.sixsq.slipstream.persistence ServiceConfiguration]
    [com.sixsq.slipstream.persistence ServiceConfigurationParameter]))

(def ^:const resource-uuid crs/service)

(def ^:const global-categories #{"SlipStream_Advanced" "SlipStream_Support" "SlipStream_Basics"})

(def user-roles-str "me ADMIN")

(def user-roles ((juxt first rest) (s/split user-roles-str #"\s+")))

(def rname->param
  {
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

   :serviceCatalogEnable       "slipstream.service.catalog.enable"})


(def param->rname (set/map-invert rname->param))

(defn param-value
  [p]
  (let [v (.getValue p)]
    (if (= (type (u/read-str v)) clojure.lang.Symbol)
      v
      (u/read-str v))))

(defn param-name
  [p]
  (get param->rname (.getName p)))

(defn param-global-valid?
  [p]
  (and (contains? global-categories (.getCategory p)) (param-name p)))

(defn sc-get-global-params
  [sc]
  (for [p (vals (.getParameters sc))]
    (when (param-global-valid? p)
      [(param-name p) (param-value p)])))

(defn build-sc-param
  [cfg-pk cfg]
  (ServiceConfigurationParameter. (rname->param cfg-pk)
                                  (str (cfg-pk cfg))
                                  ""))

(defn cfg->sc
  [cfg]
  (let [sc (ServiceConfiguration.)]
    (doseq [cfg-pk (keys cfg)]
      (if (contains? rname->param cfg-pk)
        (.setParameter sc (build-sc-param cfg-pk cfg))))
    sc))

(defn sc->cfg
  [sc]
  (into {} (sc-get-global-params sc)))

(defn throw-on-resp-error
  [resp]
  (if (> (:status resp) 400)
    (let [msg (-> resp :body :message)]
      (throw (RuntimeException. msg (ex-info msg (:body resp)))))
    resp))

(defn complete-resource
  [cfg]
  (-> cfg
      (assoc :service crs/service)
      crtpl/complete-resource))

(defn db-add-default-config
  []
  (-> cts/resource
      complete-resource
      (u/as-request resource-uuid user-roles-str)
      cr/add-impl))

(defn as-request
  [& [body]]
  (u/as-request (or body {}) resource-uuid user-roles-str))

;;
;; Interface to store and load entity as resource.
;;

(defn store
  [^ServiceConfiguration sc]
  (-> sc
      sc->cfg
      as-request
      cr/edit-impl
      throw-on-resp-error)
  sc)

(defn fetch
  []
  (-> (as-request)
      cr/retrieve-impl
      :body
      cfg->sc))

