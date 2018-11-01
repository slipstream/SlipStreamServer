(ns com.sixsq.slipstream.ssclj.resources.session-template-oidc
  "
Resource that is used to create a session using the standard OIDC workflow.
Intended for OIDC servers implemented with Keycloak.
"
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.resource-metadata :as md]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc :as session-tpl]
    [com.sixsq.slipstream.ssclj.util.metadata :as gen-md]))


(def ^:const authn-method "oidc")


(def ^:const resource-url authn-method)


;;
;; description
;;
(def ^:const desc
  p/SessionTemplateDescription)

;;
;; initialization: register this Session template
;;
(defn initialize
  []
  (p/register authn-method desc)
  (std-crud/initialize p/resource-url ::session-tpl/oidc)
  (md/register (gen-md/generate-metadata ::ns ::p/ns ::session-tpl/oidc)))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::session-tpl/oidc))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
