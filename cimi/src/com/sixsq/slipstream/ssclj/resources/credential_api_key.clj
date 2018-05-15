(ns com.sixsq.slipstream.ssclj.resources.credential-api-key
  (:require
    [com.sixsq.slipstream.auth.acl :as acl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential :as p]
    [com.sixsq.slipstream.ssclj.resources.credential-template-api-key :as tpl]
    [com.sixsq.slipstream.ssclj.resources.credential.key-utils :as key-utils]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-api-key]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c])
  (:import (clojure.lang ExceptionInfo)))

(defn strip-session-role
  [roles]
  (vec (remove #(re-matches #"^session/.*" %) roles)))

(defn extract-claims [request]
  (let [{:keys [identity roles]} (acl/current-authentication request)
        roles (strip-session-role roles)]
    (cond-> {:identity identity}
            (seq roles) (assoc :roles (vec roles)))))

(def valid-ttl? (every-pred int? pos?))

;;
;; convert template to credential: loads and validates the given SSH public key
;; provides attributes about the key.
;;
(defmethod p/tpl->credential tpl/credential-type
  [{:keys [type method ttl]} request]
  (let [[secret-key digest] (key-utils/generate)
        resource (cond-> {:resourceURI p/resource-uri
                          :type        type
                          :method      method
                          :digest      digest
                          :claims      (extract-claims request)}
                         (valid-ttl? ttl) (assoc :expiry (u/ttl->timestamp ttl)))]
    [{:secretKey secret-key} resource]))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/credential.api-key))
(defmethod p/validate-subtype tpl/credential-type
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/credential.api-key.create))
(defmethod p/create-validate-subtype tpl/credential-type
  [resource]
  (create-validate-fn resource))


;;
;; initialization: no schema for this parent resource
;;
(defn initialize
  []
  (std-crud/initialize p/resource-url :cimi/credential.api-key))

;;
;; Disable operation
;;
(defn disable-fn [{enabled :enabled id :id :as credential}]
  (if (or enabled (nil? enabled))
    (do
      (log/warn "Disabling credential : " id)
      (assoc credential :enabled false))
    (logu/log-and-throw-400 (str "Bad enabled field value " enabled))))


(defmethod p/disable-subtype tpl/credential-type
  [_ {{uuid :uuid} :params :as request}]
  (try
    (let [id (str (u/de-camelcase p/resource-name) "/" uuid)]
      (-> (db/retrieve id request)
          (a/can-modify? request)
          (disable-fn)
          (db/edit request)))
    (catch ExceptionInfo ei
      (ex-data ei))))


;; Set operation
(def set-subtype-ops-fn
  (fn [{:keys [id ] :as resource} request]
    (let [
          href-disable (str id "/disable")
          disable-op {:rel (:disable c/action-uri) :href href-disable}]
      (-> (crud/set-standard-operations resource request)
          (update-in [:operations] conj disable-op)))))

(defmethod p/set-subtype-ops tpl/credential-type
  [resource request]
  (set-subtype-ops-fn resource request))
