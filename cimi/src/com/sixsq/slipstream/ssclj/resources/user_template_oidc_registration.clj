(ns com.sixsq.slipstream.ssclj.resources.user-template-oidc-registration
  "This template allows someone to create a new account (user) from the
   oidc information"
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-oidc-registration :as oidc]
    [com.sixsq.slipstream.ssclj.resources.user-template :as p]))

(def ^:const registration-method "oidc")

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "ANON"
                            :type      "ROLE"
                            :right     "VIEW"}]})

;;
;; resource
;;

(def ^:const resource
  {:method         registration-method
   :instance       registration-method
   :name           "OIDC Registration"
   :description    "Creates a new user through oidc-registration"
   :acl            resource-acl})


;;
;; description
;;

(def ^:const desc p/UserTemplateDescription)


;;
;; initialization: register this User template
;;

(defn initialize
  []
  (p/register registration-method desc))


;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::oidc/oidc-registration))
(defmethod p/validate-subtype registration-method
  [resource]
  (validate-fn resource))
