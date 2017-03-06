(ns com.sixsq.slipstream.auth.utils.config
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [environ.core :as environ]))

(defn- find-resource
  [resource-path]
  (if-let [config-file (io/resource resource-path)]
    (do
      (log/info "using configuration file: '" (.getPath config-file) "'")
      config-file)
    (let [msg (str "resource not in classpath: '" resource-path "'")]
      (log/error msg)
      (throw (IllegalArgumentException. msg)))))

(defn- read-config
  []
  (if-let [config-name (environ/env :config-name)]
    (-> config-name
        find-resource
        slurp
        edn/read-string)
    (throw (IllegalStateException. "configuration not found"))))

(def ^:private config (memoize read-config))

(defn- read-property-value
  [prop-name & more]
  (let [v (get (config) prop-name (first more))]
    (log/debug "property" prop-name "=" v)
    v))

(defn- read-mandatory-property-value
  [prop-name]
  (if-let [v (read-property-value prop-name)]
    v
    (throw (IllegalStateException. (str "Mandatory property not defined '" prop-name "'")))))

(def property-value (memoize read-property-value))
(def mandatory-property-value (memoize read-mandatory-property-value))
