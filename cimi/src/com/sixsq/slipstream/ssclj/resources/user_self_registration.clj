(ns com.sixsq.slipstream.ssclj.resources.user-self-registration
  (:require
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.user]
    [com.sixsq.slipstream.ssclj.resources.user-template-self-registration :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; multimethods for validation
;;

(def create-validate-fn (u/create-spec-validation-fn :cimi/user-template.self-registration-create))
(defmethod p/create-validate-subtype tpl/registration-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into user resource
;; strips method attribute and updates the resource URI
;;

;; FIXME: Must check password constraints, hash the password, and setup a callback.
(defmethod p/tpl->user tpl/registration-method
  [resource request]
  (-> resource
      (assoc :resourceURI p/resource-uri)
      (assoc :isSuperUser false)
      (dissoc :passwordRepeat)))                            ;; FIXME: Encode password, check for validity.
