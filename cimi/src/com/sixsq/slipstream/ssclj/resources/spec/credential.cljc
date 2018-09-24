(ns com.sixsq.slipstream.ssclj.resources.spec.credential
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def :cimi.credential/type :cimi.credential-template/type)

(s/def :cimi.credential/method :cimi.credential-template/method)

(def credential-keys-spec (su/merge-keys-specs [c/common-attrs
                                                {:req-un [:cimi.credential/type
                                                          :cimi.credential/method]
                                                 :opt-un [:cimi.credential-template/enabled]}]))
