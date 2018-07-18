(ns com.sixsq.slipstream.ssclj.resources.configuration-session-mitreid-token
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-session-oidc :as tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-mitreid-token :as ct-mitreid-token]))


(def ^:const service "session-mitreid-token")


(def ConfigurationDescription
  tpl/desc)


;;
;; description
;;
(def ^:const desc ConfigurationDescription)


;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::ct-mitreid-token/session-mitreid-token))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))


(def create-validate-fn (u/create-spec-validation-fn ::ct-mitreid-token/session-mitreid-token-create))
(defmethod p/create-validate-subtype service
  [resource]
  (create-validate-fn resource))


;;
;; initialization
;;

(defn initialize
  []
  (std-crud/initialize p/resource-url ::ct-mitreid-token/session-mitreid-token))
