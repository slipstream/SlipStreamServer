(ns com.sixsq.slipstream.ssclj.resources.configuration-session-github
  (:require
    [clojure.tools.logging :as log]

    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-github]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-session-github :as tpl]))

(def ^:const service "session-github")

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

(def validate-fn (u/create-spec-validation-fn :cimi/configuration-template.session-github))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/configuration-template.session-github-create))
(defmethod p/create-validate-subtype service
  [resource]
  (create-validate-fn resource))
