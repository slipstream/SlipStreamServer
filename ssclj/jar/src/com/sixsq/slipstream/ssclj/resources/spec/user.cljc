(ns com.sixsq.slipstream.ssclj.resources.spec.user
  (:require
    [clojure.spec :as s]
    [clojure.spec.gen :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.user/username :cimi.core/nonblank-string)
(s/def :cimi.user/emailAddress :cimi.core/nonblank-string)
(s/def :cimi.user/firstName :cimi.core/nonblank-string)
(s/def :cimi.user/lastName :cimi.core/nonblank-string)
(s/def :cimi.user/organization :cimi.core/nonblank-string)

(s/def :cimi/user
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.user/username
                               :cimi.user/emailAddress]
                      :opt-un [:cimi.user/firstName
                               :cimi.user/lastName
                               :cimi.user/organization]}))
