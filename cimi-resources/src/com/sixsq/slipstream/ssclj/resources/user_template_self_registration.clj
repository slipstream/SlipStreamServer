(ns com.sixsq.slipstream.ssclj.resources.user-template-self-registration
  "This template allows someone to create a new account (user) from the
   minimal information: username, password, and email address."
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-self-registration]
    [com.sixsq.slipstream.ssclj.resources.user-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const registration-method "self-registration")

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
   :name           "Self Registration"
   :description    "Creates a new user through self-registration"
   :username       "username"
   :password       "password"
   :passwordRepeat "password"
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
          :password       {:displayName "Password"
                           :category    "summary"
                           :description "password"
                           :type        "password"
                           :mandatory   true
                           :readOnly    false
                           :order       21}
          :passwordRepeat {:displayName "Password (Again)"
                           :category    "summary"
                           :description "repeated password"
                           :type        "password"
                           :mandatory   true
                           :readOnly    false
                           :order       22}
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

(def validate-fn (u/create-spec-validation-fn :cimi/user-template.self-registration))
(defmethod p/validate-subtype registration-method
  [resource]
  (validate-fn resource))
