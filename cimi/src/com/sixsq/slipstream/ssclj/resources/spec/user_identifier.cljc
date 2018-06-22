(ns com.sixsq.slipstream.ssclj.resources.spec.user-identifier
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::identifier string?)

(s/def ::user ::cimi-common/resource-link)


(s/def ::user-identifier
  (su/only-keys-maps c/common-attrs
                     {:req-un [::identifier ::user]}))
