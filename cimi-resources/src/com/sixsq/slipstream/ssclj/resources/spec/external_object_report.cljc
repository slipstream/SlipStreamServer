(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-report
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object :as eo]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def :cimi.external-object.report/runUUID ::cimi-core/nonblank-string)
(s/def :cimi.external-object.report/component ::cimi-core/nonblank-string)

(def external-object-report-keys-spec
  (su/merge-keys-specs [eo/external-object-keys-spec
                        {:req-un [:cimi.external-object.report/runUUID
                                  :cimi.external-object.report/component]}]))

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        external-object-report-keys-spec]))

(s/def :cimi/external-object.report
  (su/only-keys-maps resource-keys-spec))
