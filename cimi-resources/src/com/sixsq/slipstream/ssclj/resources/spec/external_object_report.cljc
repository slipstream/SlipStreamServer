(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-report
    (:require
      [clojure.spec.alpha :as s]
      [com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report]))

;;
;; The schemas for the external-object-report-template and external-object-report
;; are currently the same. This spec is just a placeholder in case the two schemas
;; diverge.
;;

(s/def :cimi/external-object.report :cimi/external-object-template.report)
