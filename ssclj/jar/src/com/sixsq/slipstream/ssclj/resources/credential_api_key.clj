(ns com.sixsq.slipstream.ssclj.resources.credential-api-key
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.credential-api-key]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential :as p]
    [com.sixsq.slipstream.ssclj.resources.credential-template-api-key :as tpl]
    [com.sixsq.slipstream.ssclj.resources.credential.key-utils :as key-utils]))

(defn expiry-for-ttl
  [ttl]
  "20170131T10:32:00.000Z")

;;
;; convert template to credential: loads and validates the given SSH public key
;; provides attributes about the key.
;;
(defmethod p/tpl->credential tpl/credential-type
  [{:keys [type method ttl]} request]
  (let [[secret-key digest] (key-utils/generate)
        common-info (cond-> {:resourceURI p/resource-uri
                             :type        type
                             :method      method
                             :digest      digest}
                            ttl (assoc :expiry (expiry-for-ttl ttl)))]
    [{:secretKey secret-key} common-info]))

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
