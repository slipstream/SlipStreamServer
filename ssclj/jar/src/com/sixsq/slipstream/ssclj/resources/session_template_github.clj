(ns com.sixsq.slipstream.ssclj.resources.session-template-github
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-github]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const authn-method "github")

;;
;; resource
;;
(def ^:const resource
  {:method      authn-method
   :name        "GitHub"
   :description "External Authentication with GitHub Credentials"})

;;
;; description
;;
(def ^:const desc
  (merge p/SessionTemplateDescription
         {:redirectURI {:displayName "Redirect URI"
                        :category    "general"
                        :description "Redirect URI"
                        :type        "string"
                        :mandatory   true
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

(def validate-fn (u/create-spec-validation-fn :cimi/session-template.github))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
