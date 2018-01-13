(ns com.sixsq.slipstream.ssclj.resources.credential-api-key
  (:require
    [com.sixsq.slipstream.auth.acl :as acl]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-api-key]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential :as p]
    [com.sixsq.slipstream.ssclj.resources.credential-template-api-key :as tpl]
    [com.sixsq.slipstream.ssclj.resources.credential.key-utils :as key-utils]))

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
