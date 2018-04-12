(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-generic
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object :as eo]))

(def external-object-generic-keys-spec eo/external-object-keys-spec)

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        external-object-generic-keys-spec]))

(s/def :cimi/external-object.generic
  (su/only-keys-maps resource-keys-spec))
