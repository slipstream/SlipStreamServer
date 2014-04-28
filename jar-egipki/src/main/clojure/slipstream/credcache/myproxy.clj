(ns slipstream.credcache.myproxy
  "Utility functions for interacting with a MyProxy server."
  (:require
    [clojure.tools.logging :as log]
    [slipstream.credcache.utils :as u])
  (:import [org.ietf.jgss GSSCredential]
           [org.gridforum.jgss ExtendedGSSManager ExtendedGSSCredential]
           [org.globus.myproxy MyProxy InitParams GetParams DestroyParams]))

;;
;; example workflow
;;
;; (def gsscred (myproxy/credential-from-file "dummy.pem")) ;; file from VOMS example
;; (def result (myproxy/put-credential {:credential gsscred}))  ;; upload credential
;; (myproxy/get-credential result) ;; retrieve new updated credential

(def ^:const ttl-5m (* 5 60))                               ;; 5 minutes
(def ^:const ttl-4h (* 4 60 60))                            ;; 4 hours
(def ^:const ttl-12h (* 12 60 60))                          ;; 12 hours

(def ^:const default-myproxy-host "myproxy.hellasgrid.gr")
(def ^:const default-myproxy-port MyProxy/DEFAULT_PORT)


(defn credential-from-bytes
  "Loads the credential from a byte array."
  ^GSSCredential [bytes]
  (.. (ExtendedGSSManager/getInstance)
      (createCredential bytes
                        ExtendedGSSCredential/IMPEXP_OPAQUE
                        GSSCredential/DEFAULT_LIFETIME
                        nil                                 ;; uses GSI as default
                        GSSCredential/INITIATE_AND_ACCEPT)))

(defn credential-from-file
  "Loads the credential from a file."
  ^GSSCredential [file]
  (->> (u/file->byte-array file)
       (credential-from-bytes)))

(defn get-params
  "Create the GetParams object containing the parameter for renewing a
   credential via a MyProxy server.  The parameter values are passed as
   a map.  All of the parameters are optional and will default to nil,
   except lifetime which defaults to 12 hours.  The method returns the
   renewed credential.  Note that the value for the :credential key is
   the credential used to authorize the renewal."
  [{:keys [username passphrase lifetime want-trustroots cred-name credential]
    :or   {lifetime        ttl-12h
           want-trustroots false}}]
  (let [getparams
        (doto (GetParams.)
          (.setUserName username)
          (.setLifetime lifetime)
          (.setWantTrustroots want-trustroots)
          (.setCredentialName cred-name)
          (.setAuthzCreds credential))]
    (if passphrase
      (.setPassphrase getparams passphrase))
    getparams))

(defn get-proxy
  "Gets the GSS credental from the MyProxy server.  All of the parameters
   are passed as a map with the following keys: :host, :port,
   :username, and :passphrase.  Defaults can be used for all
   parameters except :username and :passphrase."
  [{:keys [myproxy-host myproxy-port lifetime credential]
    :or   {myproxy-host default-myproxy-host
           myproxy-port default-myproxy-port
           lifetime     ttl-4h}
    :as   params}]
  (let [myproxy (MyProxy. myproxy-host myproxy-port)]
    (->> params
         (merge {:myproxy-host myproxy-host
                 :myproxy-port myproxy-port
                 :lifetime     lifetime})
         (get-params)
         (.get myproxy credential))))

(defn init-params
  "Creates the InitParams object containing the parameters for initializing
   a credential on a MyProxy server.  The parameters values are passed as a
   map.  All keys are optional and will default to nil, except for lifetime
   which defaults to 12 hours."
  [{:keys [username passphrase lifetime retriever renewer cred-name cred-desc trusted-retriever]
    :or   {lifetime ttl-12h}}]
  (let [initparams
        (doto (InitParams.)
          (.setUserName username)
          (.setLifetime lifetime)
          (.setCredentialName cred-name)
          (.setCredentialDescription cred-desc)
          (.setRenewer renewer)
          (.setRetriever retriever)
          (.setTrustedRetriever trusted-retriever))]
    (if passphrase
      (.setPassphrase initparams passphrase))
    initparams))

(defn put-proxy
  "Stores a credential on a myproxy server.  The argument is a map
   with keys :myproxy-host, :myproxy-port, :lifetime, :username, :passphrase,
   :retriever, :renewer, :cred-name, :cred-desc, :trusted-retriever,
   and :credential.  All have defaults except for :credential, which must
   be provided. Method returns a map of the parameters used to store the
   credential."
  [{:keys [myproxy-host myproxy-port credential lifetime]
    :or   {myproxy-host default-myproxy-host
           myproxy-port default-myproxy-port
           lifetime     ttl-12h}
    :as   params}]
  (let [myproxy (MyProxy. myproxy-host myproxy-port)
        dn (-> credential
               (.getName)
               (.toString))
        params (merge params
                      {:myproxy-host myproxy-host
                       :myproxy-port myproxy-port
                       :lifetime     lifetime
                       :username     (get params :username dn)
                       :renewer      (get params :renewer dn)})]
    (->> params
         (init-params)
         (.put myproxy credential))
    params))

(defn destroy-params
  "Creates the DestroyParams object with the information needed to
   delete a given credential on the MyProxy server."
  [{:keys [username passphrase cred-name]}]
  (let [destroyparams
        (doto (DestroyParams.)
          (.setUserName username)
          (.setCredentialName cred-name))]
    (if passphrase
      (.setPassphrase destroyparams passphrase))
    destroyparams))

(defn destroy-proxy
  "Destroys a credential on the MyProxy server."
  [{:keys [myproxy-host myproxy-port credential]
    :or   {myproxy-host default-myproxy-host
           myproxy-port default-myproxy-port}
    :as   params}]
  (let [myproxy (MyProxy. myproxy-host myproxy-port)]
    (->> params
         (merge {:myproxy-host myproxy-host
                 :myproxy-port myproxy-port})
         (destroy-params)
         (.destroy myproxy credential))))
