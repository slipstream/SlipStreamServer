(ns com.sixsq.slipstream.ssclj.resources.configuration-session-github
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-github]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-session-github :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]))

(def ^:const service "session-github")

(def ConfigurationDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc ConfigurationDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/configuration-template.session-github))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/configuration-template.session-github-create))
(defmethod p/create-validate-subtype service
  [resource]
  (create-validate-fn resource))


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize p/resource-url :cimi/configuration-template.session-github))
