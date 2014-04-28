(ns slipstream.credcache.credential-myproxy-voms
  (:require
    [clojure.tools.logging :as log]
    [slipstream.credcache.utils :as u]
    [slipstream.credcache.common :as c]
    [slipstream.credcache.credential :as cred]
    [slipstream.credcache.myproxy :as myproxy]
    [slipstream.credcache.voms :as voms]))

(def ^:const resource-type-uri
  "http://schemas.dmtf.org/cimi/1/Credential#myproxy-voms")

(def ^:const resource-template-type-uri
  (str resource-type-uri "http://schemas.dmtf.org/cimi/1/CredentialTemplate#myproxy-voms"))

(defmethod c/template->resource resource-type-uri
           [template]
  template)

(defmethod c/validate resource-type-uri
           [resource]
  nil)

(defmethod cred/renew resource-type-uri
           [resource]
           (try
             (let [x509 (->> (myproxy/get-proxy resource)
                             (voms/gsscred->x509cred))
                   acs (voms/get-acs resource)
                   vproxy (voms/add-acs x509 acs)
                   base64 (->> (voms/proxy->bytes vproxy)
                               (u/bytes->base64 bytes))
                   expiry (voms/expiry-date vproxy)]
               (assoc resource :credential base64 :expiry expiry))
             (catch Exception e
               (log/warn "exception raised when renewing myproxy-voms credentials:" (.getMessage e))
               nil)))
