(ns com.sixsq.slipstream.ssclj.util.usercloudparamsdesc
  (:require
    [com.sixsq.slipstream.ssclj.util.config :as uc])
  (:import
    [java.util.HashMap])
  (:gen-class
    :name com.sixsq.slipstream.ssclj.util.UserCloudParamsDesc
    :methods [#^{:static true} [getDesc [java.lang.String] java.util.Map]]))

(def ^:dynamic *user-cloud-params-desc* {})

(defn- set-params!
  [cloud-name params]
  (alter-var-root #'*user-cloud-params-desc* assoc cloud-name params))

(defn kw-to-str
  [m]
  (into {}
        (for [[k v] m]
          [(name k) (if (map? v) (kw-to-str v) v)])))

(defn slurp-cloud-cred-desc
  [cloud-name]
  (uc/read-config
    (format "com/sixsq/slipstream/connector/%s-cloud-cred-desc.edn" cloud-name)))

(defn -getDesc
  [cloud-name]
  (if (and (bound? #'*user-cloud-params-desc*)
           (contains? *user-cloud-params-desc* cloud-name))
    (get *user-cloud-params-desc* cloud-name)
    (let [params-map (-> cloud-name
                         slurp-cloud-cred-desc
                         kw-to-str)]
      (set-params! cloud-name params-map)
      params-map)))