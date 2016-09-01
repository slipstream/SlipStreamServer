(ns com.sixsq.slipstream.db.serializers.service-config-impl
  (:require
    [clojure.set :as set]
    [superstring.core :as s]
    [me.raynes.fs :as fs]
    [com.sixsq.slipstream.db.serializers.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as cr]
    [com.sixsq.slipstream.ssclj.resources.configuration-slipstream :as crs]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as cts]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as crtpl])

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
    (cond
      (nil? v) ""
      (= (type (u/read-str v)) clojure.lang.Symbol) v
      :else (u/read-str v))))

(defn global-param-key
  [p]
  (get param->rname (.getName p)))

(defn param-global-valid?
  [p]
  (and (contains? global-categories (.getCategory p)) (global-param-key p)))

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

(defn sc->cfg
  [sc]
  (into {}
        (for [p (vals (.getParameters sc)) :when (param-global-valid? p)]
          [(global-param-key p) (param-value p)])))

(defn sc->cfg-desc
  [sc]
  (into {}
        (for [p (vals (.getParameters sc)) :when (param-global-valid? p)]
          [(global-param-key p) (u/desc-from-param p)])))

(defn sc->connectors
  [sc]
  (into {}
        (for [p (vals (.getParameters sc)) :when (not (param-global-valid? p))]
          [(keyword (.getName p)) (param-value p)])))

(def categires (atom #{}))

(defn sc->connectors-desc
  [sc]
  (into {}
        (for [p (vals (.getParameters sc)) :when (not (param-global-valid? p))]
          (let [k (keyword (.getName p))
                v (u/desc-from-param p)
                category (.getCategory p)]
            (when-not (contains? @categires category)
              (swap! categires conj category))
            [k v])
          )))

(defn cs->cfg-desc-and-spit
  [sc fpath]
  (let [f (fs/expand-home fpath)
        cfg-desc (sc->cfg-desc sc)]
    (with-open [^java.io.Writer w (apply clojure.java.io/writer f {})]
      (clojure.pprint/pprint cfg-desc w))))

(defn load-cfg-desc
  []
  cts/desc)

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
