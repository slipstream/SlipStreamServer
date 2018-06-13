(ns com.sixsq.slipstream.ssclj.resources.user-template-github-registration
  "This template allows someone to create a new account (user) from the
   github information"
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-github-registration :as utc]
    [com.sixsq.slipstream.ssclj.resources.user-template :as p]))

(def ^:const registration-method "github")

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
   :name           "Guthub Registration"
   :description    "Creates a new user through github-registration"
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
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::utc/github-registration))
(defmethod p/validate-subtype registration-method
  [resource]
  (validate-fn resource))
