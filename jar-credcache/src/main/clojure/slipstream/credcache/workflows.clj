(ns slipstream.credcache.workflows
  (:require
    [slipstream.credcache.utils :as u]
    [slipstream.credcache.db-utils :as db]
    [slipstream.credcache.voms-utils :as voms]
    [slipstream.credcache.myproxy-utils :as myproxy]
    [slipstream.credcache.credential :as cred])
  (:import
    [eu.emi.security.authn.x509 X509Credential]))

(def ^:const ttl-15m (* 30 60))                             ;; 30 minutes
(def ^:const ttl-7d (* 7 24 60 60))                         ;; 7 days

(defn upload-proxy
  "Uploads a VOMS proxy without attribute certificates to the myproxy
   server.  If no parameters are passed, then the defaults are used.
   Returns the map with the parameters used for the upload."
  [passphrase & [params]]
  (let [params (or params {})
        proxy-lifetime (get params :proxy-lifetime ttl-15m)]
    (->> passphrase
         (voms/generate-proxy proxy-lifetime)
         (voms/proxy->bytes)
         (myproxy/credential-from-bytes)
         (assoc params :credential)
         (myproxy/put-proxy))))

(defn upload-bootstrap-proxy
  "Uploads a short-lived VOMS proxy without any attribute certificates to
   the myproxy server under a random username/password combination.  This
   allows for the initial delegation of a proxy to the SlipStream server
   by passing the username/password.  This returns the map with the
   parameters used for the upload."
  [passphrase & [params]]
  (let [params (or params {})
        params (merge {:username       (u/random-string 8)
                       :passphrase     (u/random-string 16)
                       :renewer        nil
                       :proxy-lifetime ttl-15m}
                      params)]
    (upload-proxy passphrase params)))

(defn upload-delegation-proxy
  "Uploads a long-lived VOMS proxy without any attribute certificates
   to the myproxy server, using the DN as the username and renewer
   values.  This returns the map with the parameters used for the
   upload."
  [passphrase & [params]]
  (let [params (or params {})
        params (merge {:proxy-lifetime ttl-7d}
                      params)]
    (upload-proxy passphrase params)))

(defn renew-gsscred
  [id]
  (let [cred-info (db/retrieve-resource id)
        proxy (voms/cred-info->proxy cred-info)
        params (assoc cred-info :credential proxy)
        renewed-proxy (myproxy/get-proxy params)
        new-record (voms/add-proxy-attributes renewed-proxy)]
    (db/update-resource new-record)))

;; (def passphrase "changeme")
;; (def delegation-params (wf/upload-delegation-proxy passphrase))
;; (def bootstrap-params (wf/upload-bootstrap-proxy passphrase))
;; (def bootstrap-proxy (myproxy/get-proxy bootstrap-params))
;; (myproxy/destroy-proxy bootstrap-params)
;; (def delegated-proxy (myproxy/get-proxy {:credential bootstrap-proxy}))

(defn full-workflow
  [passphrase]
  (let [delegation-params (upload-delegation-proxy passphrase)
        bootstrap-params (upload-bootstrap-proxy passphrase)
        bootstrap-proxy (myproxy/get-proxy bootstrap-params)
        _ (myproxy/destroy-proxy bootstrap-params)
        delegated-proxy (myproxy/get-proxy {:credential bootstrap-proxy})]

    (assert ((complement nil?) delegation-params) "delegation upload failed")
    (assert ((complement nil?) bootstrap-params) "bootstrap upload failed")
    (assert ((complement nil?) bootstrap-proxy) "bootstrap proxy retrieval failed")
    (assert ((complement nil?) delegated-proxy) "delegation proxy retrieval failed")))

(defn renew-delegation
  "Renews the given credential."
  [id]
  (let [cred-info (db/retrieve-resource id)
        vproxy (voms/cred-info->proxy cred-info)
        gsscred (voms/x509cred->gsscred vproxy)
        gsscred (myproxy/get-proxy (assoc cred-info :credential gsscred))
        x509cred (voms/gsscred->x509cred gsscred)
        voms-info (get cred-info :voms {})
        vproxy (voms/include-attr-certs x509cred voms-info)
        cred-info (voms/add-proxy-attributes vproxy cred-info)]
    (db/update-resource id cred-info)))
