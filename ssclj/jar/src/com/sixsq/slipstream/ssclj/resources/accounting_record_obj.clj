(ns com.sixsq.slipstream.ssclj.resources.accounting-record-obj
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.accounting-record-vm]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.accounting_record :as p]
    ))


(def ^:const objectStore-type "obj")

;;
;; multimethods for validation
;;
(def validate-fn (u/create-spec-validation-fn :cimi/accounting-record.obj))
(defmethod p/validate-subtype objectStore-type
  [resource]
  (validate-fn resource))


