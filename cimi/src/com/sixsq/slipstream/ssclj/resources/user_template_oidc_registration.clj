(ns com.sixsq.slipstream.ssclj.resources.user-template-oidc-registration
  "This template allows someone to create a new account (user) from the
   oidc information"
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-oidc-registration :as oidc]
    [com.sixsq.slipstream.ssclj.resources.user-template :as p]))

(def ^:const registration-method "oidc-registration")

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
   :username       "username"
   :emailAddress   "user@example.com"
   :acl            resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/UserTemplateDescription
         {:username       {:displayName "Username"
                           :category    "summary"
                           :description "username"
                           :type        "string"
                           :mandatory   true
                           :readOnly    false
                           :order       20}

          :emailAddress   {:displayName "Email Address"
                           :category    "general"
                           :description "email address"
                           :type        "string"
                           :mandatory   true
                           :readOnly    false
                           :order       23}}))

;;
;; initialization: register this User template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::oidc/oidc-registration))
(defmethod p/validate-subtype registration-method
  [resource]
  (validate-fn resource))
