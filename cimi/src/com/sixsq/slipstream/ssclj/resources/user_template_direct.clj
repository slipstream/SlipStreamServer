(ns com.sixsq.slipstream.ssclj.resources.user-template-direct
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-direct :as user-tpl]
    [com.sixsq.slipstream.ssclj.resources.user-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]))

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
   :instance     registration-method
   :name         "Direct"
   :description  "Direct creation of user by the administrator"
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
;; initialization: register this user template and create direct registration template
;;

(defn initialize
  []
  (p/register registration-method desc)
  (std-crud/initialize p/resource-url ::user-tpl/direct)
  (std-crud/add-if-absent (str p/resource-url "/" registration-method) p/resource-url resource))


;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::user-tpl/direct))
(defmethod p/validate-subtype registration-method
  [resource]
  (validate-fn resource))
