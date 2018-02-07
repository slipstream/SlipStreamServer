(ns com.sixsq.slipstream.ssclj.resources.spec.email
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.email/address :cimi.core/email)
(s/def :cimi.email/validated? boolean?)

(s/def :cimi/email
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.email/address
                               :cimi.email/validated?]}))
