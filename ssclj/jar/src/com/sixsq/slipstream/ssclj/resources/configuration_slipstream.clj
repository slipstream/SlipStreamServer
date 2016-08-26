(ns com.sixsq.slipstream.ssclj.resources.configuration-slipstream
  (:require
    [com.sixsq.slipstream.ssclj.resources.configuration :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const service "slipstream")

;;
;; schemas
;;

(def ConfigurationAttrs
  tpl/ConfigurationTemplateAttrs)

(def Configuration
  (merge p/Configuration
         ConfigurationAttrs))

(def ConfigurationCreate
  (merge c/CreateAttrs
         {:configurationTemplate tpl/ConfigurationTemplateRef}))

(def ConfigurationDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc ConfigurationDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-validation-fn Configuration))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-validation-fn ConfigurationCreate))
(defmethod p/create-validate-subtype service
  [resource]
  (create-validate-fn resource))
