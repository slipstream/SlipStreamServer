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
    (java.util.logging Logger)
    (com.sixsq.slipstream.persistence ServiceConfiguration)
    (com.sixsq.slipstream.persistence ServiceConfigurationParameter)))


(def logger (Logger/getLogger "com.sixsq.slipstream.db.serializers"))

(def ^:const resource-uuid crs/service)

(def ^:const global-categories #{"SlipStream_Advanced" "SlipStream_Support" "SlipStream_Basics"})

(def user-roles-str "me ADMIN")

(def user-roles ((juxt first rest) (s/split user-roles-str #"\s+")))


(defn cloud-connector-class-value
  []
  "l1:local")

(defn slipstream-version-value
  []
  "3.10")

(defn assoc-extra-params-vals
  [m]
  (-> m
      (assoc :cloudConnectorClass (cloud-connector-class-value))
      (assoc :slipstreamVersion (slipstream-version-value))))

(def extra-params-desc
  {:cloudConnectorClass
   {:displayName  "cloud.connector.class"
    :type         "Text"
    :category     "SlipStream_Basics"
    :description  "Cloud connector java class name(s) (comma separated for multi-cloud configuration)"
    :mandatory    true
    :readOnly     false
    :instructions ""
    :order        0}
   :slipstreamVersion
   {:displayName  "slipstream.version"
    :type         "string"
    :category     "SlipStream_Advanced"
    :description  "Installed SlipStream version"
    :mandatory    true
    :readOnly     true
    :instructions ""
    :order        0}})

(def extra-rname->param
  {:cloudConnectorClass "cloud.connector.class"
   :slipstreamVersion   "slipstream.version"})

(def base-rname->param
  {:serviceURL                 "slipstream.base.url"
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

(def rname->param (merge base-rname->param extra-rname->param))

(def param->rname (set/map-invert rname->param))

(defn param-value
  [p]
  (let [v (.getValue p)]
    (cond
      (nil? v) ""
      (and (= (type v) java.lang.String) (empty? v)) ""
      (= (type (u/read-str v)) clojure.lang.Symbol) v
      :else (u/read-str v))))

(defn global-param-key
  [p]
  (get param->rname (.getName p)))

(defn category-global?
  [p]
  (contains? global-categories (.getCategory p)))

(defn param-global-valid?
  [p]
  (and (category-global? p) (global-param-key p)))

(defn cfg->sc
  ([cfg]
   (cfg->sc cfg {}))
  ([cfg cfg-desc]
   (let [sc (ServiceConfiguration.)]
     (doseq [pk (keys cfg)]
       (if (contains? rname->param pk)
         (let [value (pk cfg)
               desc  (assoc (or (pk cfg-desc) {}) :name (pk rname->param))]
           (.setParameter sc (u/build-sc-param value desc)))))
     sc)))

(defn param-for-sc->cfg?
  [p]
  (and (param-global-valid? p)
       (not (contains? (-> extra-rname->param vals set) (.getName p)))))

(defn sc->cfg
  [sc]
  (into {}
        (for [p (vals (.getParameters sc)) :when (param-for-sc->cfg? p)]
          [(global-param-key p) (param-value p)])))

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

(defn sc->connector
  [sc category]
  (into {}
        (for [p (vals (.getParameters sc)) :when (non-gobal-category-match? p category)]
          [(keyword (.getName p)) (param-value p)])))

(defn sc->connector-desc
  [sc category]
  (into {}
        (for [p (vals (.getParameters sc)) :when (non-gobal-category-match? p category)]
          [(keyword (.getName p))
           (u/desc-from-param p)])))

(defn sc->connector-with-desc
  [sc category]
  [(sc->connector sc category)
   (sc->connector-desc sc category)])

(defn sc->connectors
  "Given ServiceConfiguration, returns the following map
  {:connector-instance-name [{:param val
                              ..}
                             {:param {description map}
                              ..}]
   ..}
   "
  [sc & [categories]]
  (into {}
        (for [c (or categories (non-global-categories sc))]
          [(keyword c) (sc->connector-with-desc sc c)])))

(defn cs->cfg-desc-and-spit
  [sc fpath]
  (let [f        (fs/expand-home fpath)
        cfg-desc (sc->cfg-desc sc)]
    (with-open [^java.io.Writer w (apply clojure.java.io/writer f {})]
      (clojure.pprint/pprint cfg-desc w))))

(defn load-cfg-desc
  []
  (merge cts/desc extra-params-desc))

(defn complete-resource
  [cfg]
  (-> cfg
      (assoc :service crs/service)
      crtpl/complete-resource))

(defn db-add-default-config
  [& [fail]]
  (.info logger "Adding default server configuration to ES DB.")
  (let [resp (-> cts/resource
                 complete-resource
                 (u/as-request resource-uuid user-roles-str)
                 cr/add-impl)]
    (if (and fail (> (:status resp) 400))
      (u/throw-on-resp-error resp)
      (do
        (.warning logger (str "Failure adding default configuration: " resp))))))

(defn as-request
  [& [body]]
  (u/as-request (or body {}) resource-uuid user-roles-str))
