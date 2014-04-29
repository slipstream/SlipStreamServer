(ns slipstream.credcache.credential-myproxy-voms
  (:require
    [clojure.tools.logging :as log]
    [slipstream.credcache.utils :as u]
    [slipstream.credcache.common :as c]
    [slipstream.credcache.myproxy-utils :as myproxy]
    [slipstream.credcache.voms-utils :as voms]
    [slipstream.credcache.renewal :as r]))

(def ^:const resource-type-uri
  "http://schemas.dmtf.org/cimi/1/Credential#myproxy-voms")

(def ^:const resource-template-type-uri
  "http://schemas.dmtf.org/cimi/1/CredentialTemplate#myproxy-voms")

(defmethod c/template->resource resource-template-type-uri
           [template]
  (let [gsscred (myproxy/get-proxy template)
        voms-info (:voms template)
        vproxy (voms/gsscred->vproxy gsscred voms-info)]
    (try
      (myproxy/destroy-proxy (assoc template :proxy gsscred))
      (catch Exception e
        (log/warn "unable to delete bootstrap credential:" (.getMessage e))))

    (-> template
        (voms/add-proxy-attributes vproxy)
        (c/update-resource-typeuri)
        (dissoc :username :passphrase))))

(defmethod c/validate-template resource-template-type-uri
           [resource]
  resource)

(defmethod c/validate resource-type-uri
           [resource]
  resource)

(defmethod r/renew resource-type-uri
           [resource]
  (try
    (let [gsscred (myproxy/get-proxy resource)
          voms-info (:voms resource)
          vproxy (voms/gsscred->vproxy gsscred voms-info)]
      (voms/add-proxy-attributes resource vproxy))
    (catch Exception e
      (log/warn "exception raised when renewing myproxy-voms credentials:" (.getMessage e))
      nil)))
