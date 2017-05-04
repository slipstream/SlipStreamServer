(ns com.sixsq.slipstream.ssclj.resources.user-direct
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.user]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-direct]
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.user-template-direct :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const registration-method "direct")

;;
;; validate the create resource
;;
(def create-validate-fn (u/create-spec-validation-fn :cimi/user-template.direct-create))
(defmethod p/create-validate-subtype registration-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into user resource
;; just strips method attribute and updates the resource URI
;;
(defmethod p/tpl->user registration-method
  [resource request]
  (-> resource
      (dissoc :method)
      (assoc :resourceURI p/resource-uri)))

