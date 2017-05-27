(ns com.sixsq.slipstream.ssclj.resources.session-template-internal
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-internal]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.db.impl :as db]))

(def ^:const authn-method "internal")

(def default-template {:method      authn-method
                       :methodKey   authn-method
                       :name        "Internal"
                       :description "Internal Authentication via Username/Password"
                       :username    "username"
                       :password    "password"
                       :acl         p/resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/SessionTemplateDescription
         {:username {:displayName "Username"
                     :category    "general"
                     :description "username"
                     :type        "string"
                     :mandatory   true
                     :readOnly    false
                     :order       3}
          :password {:displayName "Password"
                     :category    "general"
                     :description "password"
                     :type        "password"
                     :mandatory   true
                     :readOnly    false
                     :order       4}}))

;;
;; initialization: register this Session template and create internal authentication template
;;
(defn initialize
  []
  (p/register authn-method desc)
  (try
    (db/add p/resource-name default-template {:user-name "INTERNAL", :user-roles ["ADMIN"]})
    (log/info "Created session-template/internal resource.")
    (catch Exception e
      (log/warn "session-template/internal resource not created; may already exist; message: " (str e)))))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session-template.internal))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
