(ns com.sixsq.slipstream.ssclj.resources.configuration-template-nuvlabox-identifier
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-nuvlabox-identifier :as ct-nbid]))

(def ^:const service "nuvlabox-identifier")

;;
;; resource
;;
(def ^:const resource
  {:service      service
   :name         "NuvlaBox Unique Identifiers"
   :description  "NuvlaBox Unique Identifiers"
   :instance     "names"
   :identifiers  [{:name "John Doe"} {:name "Jane Dow"}] })


;;
;; description
;;
(def ^:const desc
  (merge p/ConfigurationTemplateDescription
         {:identifiers  {:displayName "Identifiers"
                         :type        "list"
                         :description "list of unique identifiers that can be assigned to NuvlaBox"
                         :mandatory   true
                         :readOnly    false
                         :order       20}}))

;;
;; initialization: register this Configuration template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::ct-nbid/nuvlabox-identifier))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))
