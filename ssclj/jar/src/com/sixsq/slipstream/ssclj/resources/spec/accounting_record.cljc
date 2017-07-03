(ns com.sixsq.slipstream.ssclj.resources.spec.accounting-record
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.accounting-record/identifier :cimi.core/identifier)
(s/def :cimi.accounting-record/start :cimi.core/timestamp)
(s/def :cimi.accounting-record/stop :cimi.core/timestamp)
(s/def :cimi.accounting-record/user :cimi.core/nonblank-string)
(s/def :cimi.accounting-record/cloud :cimi.core/nonblank-string)
(s/def :cimi.accounting-record/roles (s/coll-of :cimi.core/nonblank-string :min-count 1))
(s/def :cimi.accounting-record/groups (s/coll-of :cimi.core/nonblank-string :min-count 1))
(s/def :cimi.accounting-record/realm :cimi.core/nonblank-string)
(s/def :cimi.accounting-record/module :cimi.core/nonblank-string)
(s/def :cimi.accounting-record/serviceOffer :cimi.common/resource-link)


(s/def :cimi.accounting-record/type :cimi.core/identifier)

(def account-keys-specs {:req-un [:cimi.accounting-record/identifier
                                  :cimi.accounting-record/start
                                  :cimi.accounting-record/user
                                  :cimi.accounting-record/cloud
                                  :cimi.accounting-record/serviceOffer
                                  :cimi.account-record/type]

                         :opt-un [:cimi.accounting-record/stop
                                  :cimi.accounting-record/roles
                                  :cimi.accounting-record/groups
                                  :cimi.accounting-record/realm
                                  :cimi.accounting-record/module]})

(def accounting-record-keys-spec (su/merge-keys-specs [c/common-attrs
                                                       account-keys-specs]))

