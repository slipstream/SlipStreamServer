(ns com.sixsq.slipstream.ssclj.resources.spec.callback
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def ::action ::cimi-core/nonblank-string)
(s/def ::state #{"WAITING" "FAILED" "SUCCEEDED"})
(s/def ::targetResource ::cimi-common/resource-link)
(s/def ::data (su/constrained-map keyword? any?))
(s/def ::expires ::cimi-core/timestamp)

(s/def ::callback
  (su/only-keys-maps cimi-common/common-attrs
                     {:req-un [::action
                               ::state]
                      :opt-un [::targetResource
                               ::data
                               ::expires]}))
