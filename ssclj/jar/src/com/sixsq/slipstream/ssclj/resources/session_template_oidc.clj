(ns com.sixsq.slipstream.ssclj.resources.session-template-oidc
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const authn-method "oidc")

;;
;; resource
;;
(def ^:const resource
  {:method      authn-method
   :name        "OpenID Connect"
   :description "External Authentication via OpenID Connect Protocol"})

;;
;; description
;;
(def ^:const desc
  (merge p/SessionTemplateDescription
         {:redirectURI {:displayName "Redirect URI"
                        :category    "general"
                        :description "Redirect URI"
                        :type        "hidden"
                        :mandatory   false
                        :readOnly    false
                        :order       1}}))

;;
;; initialization: register this Session template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session-template.oidc))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
