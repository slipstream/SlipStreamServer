(ns com.sixsq.slipstream.ssclj.resources.user-template-mitreid-registration
  "This template allows someone to create a new account (user) from the
   MITREid information"
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-mitreid-registration :as mitreid]
    [com.sixsq.slipstream.ssclj.resources.user-template :as p]))

(def ^:const registration-method "mitreid")

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
   :name           "MITREid Registration"
   :description    "Creates a new user through mitreid-registration"
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

(def validate-fn (u/create-spec-validation-fn ::mitreid/mitreid-registration))
(defmethod p/validate-subtype registration-method
  [resource]
  (validate-fn resource))
