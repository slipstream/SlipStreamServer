(ns com.sixsq.slipstream.ssclj.resources.user-direct
  (:require
    [com.sixsq.slipstream.auth.internal :as ia]
    [com.sixsq.slipstream.ssclj.resources.spec.user]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-direct]
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.user-template-direct :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; validate the create resource
;;

(def create-validate-fn (u/create-spec-validation-fn :cimi/user-template.direct-create))
(defmethod p/create-validate-subtype tpl/registration-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into user resource
;; just updates the resource URI and sets isSuperUser if not set
;;
(defmethod p/tpl->user tpl/registration-method
  [{:keys [isSuperUser] :as resource} request]
  (cond-> (assoc resource :resourceURI p/resource-uri)
          (nil? isSuperUser) (assoc :isSuperUser false)))
