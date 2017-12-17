(ns com.sixsq.slipstream.ssclj.resources.spec.callback
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.callback/action :cimi.core/nonblank-string)
(s/def :cimi.callback/state #{"WAITING" "FAILED" "SUCCESS"})
(s/def :cimi.callback/resource :cimi.common/resource-link)
(s/def :cimi.callback/data (su/constrained-map keyword? any?))
(s/def :cimi.common/expires :cimi.core/timestamp)

(s/def :cimi/callback
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.callback/action
                               :cimi.callback/state
                               :cimi.callback/resource]
                      :opt-un [:cimi.callback/data
                               :cimi.callback/expires]}))
