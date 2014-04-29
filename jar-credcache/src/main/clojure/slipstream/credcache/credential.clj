(ns slipstream.credcache.credential
  "Management functions for credential resources within the database."
  (:require
    [clojure.tools.logging :as log]
    [clj-time.core :as t]
    [schema.core :as s]
    [slipstream.credcache.db-utils :as db]
    [slipstream.credcache.utils :as u]
    [slipstream.credcache.common :as c]
    [slipstream.credcache.job :as j]))

;;
;; utilities
;;

(def ^:const prefix "credential/")

(defn uuid->id
  [uuid]
  (str prefix uuid))

;;
;; credential schema
;;

;;
;; Resources can have the following common keys:
;;
;; :id (required) -- id of the resource
;; :name -- short name/title of resource
;; :description -- longer description of resource
;; :created (required) -- creation timestamp of resource
;; :updated (required) -- late update timestamp of resource
;;
;; :properties -- string/string map of user information
;;
;; All credentials can/must have the following keys:
;;
;; :typeURI (required) -- type of the resource/credential
;; :expiry (optional) -- expiry date of the credential
;;
;; For voms-proxy credentials:
;; :voms -- map of voms information
;; :myproxy-host -- hostname of myproxy server
;; :myproxy-port -- port of myproxy server
;;

(def CredentialAttributes
  {(s/optional-key :expiry) s/Int})

(def Credential
  (merge c/CommonAttributes CredentialAttributes))

(def MyProxyVomsCredential
  {:myproxy-host s/Str
   :myproxy-port s/Int
   :credential   s/Str})

(def VomsAttributes
  {s/Str {(s/optional-key :fqans)   [s/Str]
          (s/optional-key :targets) [s/Str]}})

(def CredentialTemplate
  (merge c/CommonAttributes
         {:myproxy-host s/Str
          :myproxy-port s/Int
          :username     s/Str
          :password     s/Str}))

;;
;; standard CRUD functions for credentials
;;

(defn create
  "Adds a new credential to the database given the information in the
   template; returns the id of the created credential."
  [template]
  (let [resource (-> template
                     (c/validate-template)
                     (c/template->resource))]
    (->> (u/random-uuid)
         (uuid->id)
         (assoc resource :id)
         (c/update-timestamps)
         (c/validate)
         (db/create-resource)
         (j/schedule-renewal)
         (:id))))

(defn retrieve
  [id]
  (db/retrieve-resource id))

(defn update
  "Updates an existing credential in the database.  The identifier
   must be part of the credential itself under the :id key. Returns
   the updated resource."
  [resource]
  (->> resource
       (c/update-timestamps)
       (c/validate)
       (db/update-resource)))

(defn delete
  "Removes the credential associated with the id from the database."
  [id]
  (db/delete-resource id))

(defn resource-ids
  "Provides a list of all of the credential ids in the database."
  [])
