;;
;; TODO: factorize common libraries to auth and ssclj project
;;
(ns com.sixsq.slipstream.ssclj.util.config
  (:require [environ.core           :as environ]
            [clojure.edn            :as edn]
            [clojure.tools.logging  :as log]
            [clojure.java.io        :as io]))

(defn- find-resource
  [resource-path]
  (if-let [config-file (io/resource resource-path)]
    (do
      (log/info "Will use "(.getPath config-file)" as config file")
      config-file)
    (let [msg (str "Resource not found (must be in classpath): '" resource-path "'")]
      (log/error msg)
      (throw (IllegalArgumentException. msg)))))

(defn- read-config
  []
  (if-let [config-path (environ/env :config-path)]
    (-> config-path
        find-resource
        slurp
        edn/read-string)
    (throw (IllegalStateException. "No configuration found."))))

(def ^:private config (memoize read-config))

(defn- read-property-value
  [name & more]
  (let [v (get (config) name (first more))]
    (log/info "property" name "=" v)
    v))

(def property-value (memoize read-property-value))