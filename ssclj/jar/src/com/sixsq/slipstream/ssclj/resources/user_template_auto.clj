(ns com.sixsq.slipstream.ssclj.resources.user-template-auto
  "This UserTemplate allows a user to register a new account automatically."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-auto]
    [com.sixsq.slipstream.ssclj.resources.user-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const registration-method "auto")

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "ANON"
                            :type      "ROLE"
                            :right     "VIEW"}]})

;;
;; resource
;;
(def ^:const resource
  {:method       registration-method
   :name         "Auto"
   :description  "Creates a new user through self-registration"
   :username     "username"
   :password     "password"
   :firstName    "John"
   :lastName     "Doe"
   :emailAddress "user@example.com"
   :organization ""
   :roles        ""
   :acl          resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/UserTemplateDescription
         {:username     {:displayName "Username"
                         :category    "summary"
                         :description "username"
                         :type        "string"
                         :mandatory   true
                         :readOnly    false
                         :order       20}
          :firstName    {:displayName "First name"
                         :category    "summary"
                         :description "First name"
                         :type        "string"
                         :mandatory   true
                         :readOnly    false
                         :order       21}
          :lastName     {:displayName "Last name"
                         :category    "summary"
                         :description "Last name"
                         :type        "string"
                         :mandatory   true
                         :readOnly    false
                         :order       22}
          :organization {:displayName "Organization"
                         :category    "summary"
                         :description "First name"
                         :type        "string"
                         :mandatory   true
                         :readOnly    false
                         :order       23}
          :emailAddress {:displayName "Email Address"
                         :category    "general"
                         :description "email address"
                         :type        "string"
                         :mandatory   true
                         :readOnly    false
                         :order       24}}))

;;
;; initialization: register this User template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/user-template.auto))
(defmethod p/validate-subtype registration-method
  [resource]
  (validate-fn resource))
