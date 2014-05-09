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

(def ^:const resource-type "Credential")

(def ^:const resource-type-uri "http://schemas.dmtf.org/cimi/1/Credential")

(def ^:const resource-template-type-uri "http://schemas.dmtf.org/cimi/1/CredentialTemplate")

(defn uuid->id
  [uuid]
  (str resource-type "-" uuid))

;;
;; credential schema
;;

(def CommonCredentialAttributes
  {:subtypeURI              s/Str
   (s/optional-key :expiry) s/Int})

(def Credential
  (merge c/CommonResourceAttributes CommonCredentialAttributes))

(def CredentialTemplate
  (merge c/CommonTemplateAttributes CommonCredentialAttributes))

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
  []
  (db/all-resource-ids resource-type))

;;
;; utility functions
;;

(defn reschedule-all-renewals
  "Should be called on system startup to read all of the defined credentials
   and reschedule the renewal jobs."
  []
  (doall
    (->> (resource-ids)
         (map retrieve)
         (remove nil?)
         (map j/schedule-renewal))))

(defn schedule-cleanup
  []
  (j/schedule-cache-cleanup resource-type))
