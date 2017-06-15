(ns com.sixsq.slipstream.ssclj.resources.configuration-session-oidc
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-oidc]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-session-oidc :as tpl]))

(def ^:const service "session-oidc")

(def ^:const resource-url (str p/resource-url "/" service))

(def ConfigurationDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc ConfigurationDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/configuration-template.session-oidc))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/configuration-template.session-oidc-create))
(defmethod p/create-validate-subtype service
  [resource]
  (create-validate-fn resource))
