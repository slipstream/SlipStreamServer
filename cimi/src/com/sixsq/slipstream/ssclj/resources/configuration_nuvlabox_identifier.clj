(ns com.sixsq.slipstream.ssclj.resources.configuration-nuvlabox-identifier
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-nuvlabox-identifier :as tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-nuvlabox-identifier :as ct-nbid]))

(def ^:const service "nuvlabox-identifier")

(def ConfigurationDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc ConfigurationDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::ct-nbid/nuvlabox-identifier))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn ::ct-nbid/nuvlabox-identifier-create))
(defmethod p/create-validate-subtype service
  [resource]
  (create-validate-fn resource))


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize p/resource-url ::ct-nbid/nuvlabox-identifier))
