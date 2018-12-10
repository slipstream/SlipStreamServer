(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-public
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object :as eo]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(def external-object-public-keys-spec eo/common-external-object-attrs)

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        external-object-public-keys-spec]))

(s/def ::external-object
  (su/only-keys-maps resource-keys-spec))