(ns com.sixsq.slipstream.ssclj.resources.spec.user
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.user/username :cimi.core/nonblank-string)
(s/def :cimi.user/emailAddress :cimi.core/nonblank-string)
(s/def :cimi.user/firstName :cimi.core/nonblank-string)
(s/def :cimi.user/lastName :cimi.core/nonblank-string)
(s/def :cimi.user/organization string?)

(def user-keys-spec
  {:req-un [:cimi.user/username
            :cimi.user/emailAddress]
   :opt-un [:cimi.user/firstName
            :cimi.user/lastName
            :cimi.user/organization]})

(s/def :cimi/user
  (su/only-keys-maps c/common-attrs
                     user-keys-spec))
