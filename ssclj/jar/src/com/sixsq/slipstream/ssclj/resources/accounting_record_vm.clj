(ns com.sixsq.slipstream.ssclj.resources.accounting-record-vm
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.accounting-record-vm]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.accounting_record :as p]
    ))


(def ^:const vm-type "vm")

;;
;; multimethods for validation
;;
(def validate-fn (u/create-spec-validation-fn :cimi/accounting-record.vm))
(defmethod p/validate-subtype vm-type
  [resource]
  (validate-fn resource))


