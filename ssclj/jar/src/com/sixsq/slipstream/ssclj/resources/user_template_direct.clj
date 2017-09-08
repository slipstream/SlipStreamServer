(ns com.sixsq.slipstream.ssclj.resources.user-template-direct
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-direct]
    [com.sixsq.slipstream.ssclj.resources.user-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const registration-method "direct")

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "ADMIN"
                            :type      "ROLE"
                            :right     "VIEW"}]})

;;
;; resource
;;
(def ^:const resource
  {:method       registration-method
   :name         "Direct"
   :description  "Direct creation of user by the administrator"
   :username     "username"
   :emailAddress "user@example.com"
   :acl          resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/UserTemplateDescription
         {:username     {:displayName "Username"
                         :category    "general"
                         :description "username"
                         :type        "string"
                         :mandatory   true
                         :readOnly    false
                         :order       20}
          :emailAddress {:displayName "Email Address"
                         :category    "general"
                         :description "email address"
                         :type        "string"
                         :mandatory   true
                         :readOnly    false
                         :order       21}
          :firstName    {:displayName "First Name"
                         :category    "general"
                         :description "user's given name"
                         :type        "string"
                         :mandatory   false
                         :readOnly    false
                         :order       22}
          :lastName     {:displayName "Last Name"
                         :category    "general"
                         :description "user's last or family name"
                         :type        "string"
                         :mandatory   false
                         :readOnly    false
                         :order       23}
          :organization {:displayName "Organization"
                         :category    "general"
                         :description "user's organization"
                         :type        "string"
                         :mandatory   false
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

(def validate-fn (u/create-spec-validation-fn :cimi/user-template.direct))
(defmethod p/validate-subtype registration-method
  [resource]
  (validate-fn resource))
