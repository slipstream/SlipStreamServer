(ns com.sixsq.slipstream.ssclj.resources.credential-template-username-password
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-username-password]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const credential-type "username-password")

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "USER"
                            :type      "ROLE"
                            :right     "VIEW"}]})

;;
;; resource
;;
(def ^:const resource
  {:type        credential-type
   :name        "Username/Password"
   :description "username and clear-text password pair"
   :username    "username"
   :password    "clear-text"
   :acl         resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/CredentialTemplateDescription
         {:username {:displayName "Username"
                     :category    "general"
                     :description "username"
                     :type        "string"
                     :mandatory   true
                     :readOnly    false
                     :order       1}
          :password {:displayName "Password"
                     :category    "general"
                     :description "password"
                     :type        "string"
                     :mandatory   true
                     :readOnly    false
                     :order       2}}))

;;
;; initialization: register this Credential template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/credential-template.username-password))
(defmethod p/validate-subtype credential-type
  [resource]
  (validate-fn resource))
