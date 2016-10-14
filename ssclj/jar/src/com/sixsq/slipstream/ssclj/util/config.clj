;;
;; TODO: factorize common libraries to auth and ssclj project
;;
(ns com.sixsq.slipstream.ssclj.util.config
  (:require [environ.core :as environ]
            [me.raynes.fs :as fs]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(defn- find-resource
  [resource-path]
  (if-let [config-file (io/resource resource-path)]
    config-file
    (let [msg (str "Resource not found (must be in classpath): '" resource-path "'")]
      (log/error msg)
      (throw (IllegalArgumentException. msg)))))

(defn find-file
  [fpath]
  (if (fs/exists? (fs/expand-home fpath))
    fpath
    (find-resource fpath)))

(defn read-config
  [& [path]]
  (if-let [config-path (or path (environ/env :config-path))]
    (-> config-path
        find-file
        slurp
        edn/read-string)
    (throw (IllegalStateException. "No configuration found."))))

(def ^:private config (memoize read-config))

(defn- read-property-value
  [name & more]
  (let [v (get (config) name (first more))]
    (log/debug "property" name "=" v)
    v))

(def property-value (memoize read-property-value))
