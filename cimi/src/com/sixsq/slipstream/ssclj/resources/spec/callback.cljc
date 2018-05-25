(ns com.sixsq.slipstream.ssclj.resources.spec.callback
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def :cimi.callback/action ::cimi-core/nonblank-string)
(s/def :cimi.callback/state #{"WAITING" "FAILED" "SUCCEEDED"})
(s/def :cimi.callback/targetResource ::cimi-common/resource-link)
(s/def :cimi.callback/data (su/constrained-map keyword? any?))
(s/def :cimi.callback/expires ::cimi-core/timestamp)

(s/def :cimi/callback
  (su/only-keys-maps cimi-common/common-attrs
                     {:req-un [:cimi.callback/action
                               :cimi.callback/state]
                      :opt-un [:cimi.callback/targetResource
                               :cimi.callback/data
                               :cimi.callback/expires]}))
