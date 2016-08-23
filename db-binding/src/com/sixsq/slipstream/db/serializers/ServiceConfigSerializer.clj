(ns com.sixsq.slipstream.db.serializers.ServiceConfigSerializer
  (:require
    [clojure.set :as set]
    [superstring.core :as s]
    [com.sixsq.slipstream.ssclj.resources.configuration :as cr]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih]
    [com.sixsq.slipstream.db.impl :as db])
  (:import
    [com.sixsq.slipstream.persistence ServiceConfiguration]
    [com.sixsq.slipstream.persistence ServiceConfigurationParameter])
  (:gen-class
    :methods [#^{:static true} [store [com.sixsq.slipstream.persistence.ServiceConfiguration] com.sixsq.slipstream.persistence.ServiceConfiguration]
              #^{:static true} [load [] com.sixsq.slipstream.persistence.ServiceConfiguration]]))

(def acl
  {:owner {:principal "ADMIN"
           :type      "ROLE"}
   :rules [{:principal "ADMIN"
            :type      "ROLE"
            :right     "MODIFY"}]}
  )

(def ^:const resource-uuid "slipstream")

(def user-roles-str "me ADMIN")

(def user-roles ((juxt first rest) (s/split user-roles-str #"\s+")))

(def valid-acl {:owner {:principal "me" :type "USER"}})

(def global-categories #{"SlipStream_Advanced" "SlipStream_Support" "SlipStream_Basics"})

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

(defn sc-get-global-params
  [sc]
  (for [p (vals (.getParameters sc))]
    (when (param-global-valid? p)
      [(param-name p) (param-value p)])))

(defn set-id
  [cfg]
  (assoc cfg :id (str cr/resource-url "/slipstream")))

(defn sc->cfg
  [sc]
  (into {} (sc-get-global-params sc)))

(defn as-request
  [cfg]
  (let [request {:params  {:uuid resource-uuid}
                 :body    cfg
                 :headers {aih/authn-info-header user-roles-str}}]
    ((aih/wrap-authn-info-header identity) request)))

(defn throw-on-resp-error
  [resp]
  (if (> (:status resp) 400)
    (let [msg (-> resp :body :message)]
      (throw (RuntimeException. msg (ex-info msg (:body resp)))))
    resp))

(defn display
  [d & [msg]]
  (println msg)
  (clojure.pprint/pprint d)
  d)

(defn db-dump
  []
  (clojure.pprint/pprint (com.sixsq.slipstream.db.es.es-util/dump
                           com.sixsq.slipstream.db.es.es-binding/*client*
                           com.sixsq.slipstream.db.es.es-binding/index-name cr/resource-url)))

(defn -store
  [^ServiceConfiguration sc]
  (-> sc
      sc->cfg
      set-id
      as-request
      cr/edit-impl
      throw-on-resp-error)
  sc)

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

(defn -load
  []
  (let [cfg (db/retrieve "configuration/slipstream" {:user-name "konstan" :user-roles ["ADMIN"]})]
    (cfg->sc cfg)))
