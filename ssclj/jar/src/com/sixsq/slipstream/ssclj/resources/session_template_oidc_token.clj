(ns com.sixsq.slipstream.ssclj.resources.session-template-oidc-token
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const authn-method "oidc")

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
  (p/register authn-method desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session-template.oidc))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
