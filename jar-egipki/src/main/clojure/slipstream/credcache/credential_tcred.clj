(ns slipstream.credcache.credential-tcred
  (:require
    [clojure.tools.logging :as log]
    [slipstream.credcache.utils :as u]
    [slipstream.credcache.common :as c]
    [slipstream.credcache.credential :as cred]
    [slipstream.credcache.myproxy :as myproxy]
    [slipstream.credcache.voms :as voms]))

(def ^:const resource-type-uri
  "http://schemas.dmtf.org/cimi/1/Credential#tcred")

(def ^:const resource-template-type-uri
  (str resource-type-uri "http://schemas.dmtf.org/cimi/1/CredentialTemplate#tcred"))

(defmethod c/template->resource resource-type-uri
           [template]
  template)

(defmethod c/validate resource-type-uri
           [resource]
           nil)

(defmethod cred/renew resource-type-uri
           [resource]
  resource)
