(ns com.sixsq.slipstream.ssclj.resources.spec.email
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::address ::cimi-core/email)
(s/def ::validated? boolean?)


(s/def ::email
  (su/only-keys-maps c/common-attrs
                     {:req-un [::address
                               ::validated?]}))
