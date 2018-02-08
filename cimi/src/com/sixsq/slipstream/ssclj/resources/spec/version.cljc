(ns com.sixsq.slipstream.ssclj.resources.spec.version
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))


(s/def :cimi.version/name :cimi.common/name)
(s/def :cimi.version/path :cimi.core/nonblank-string)
(s/def :cimi.version/parent string?)
(s/def :cimi.version/description string?)
(s/def :cimi.version/type #{"project" "component" "application"})


(s/def :cimi/version
  (su/constrained-map keyword? any?
                      c/common-attrs
                      {:req-un [:cimi.version/name
                                :cimi.version/path
                                :cimi.version/type
                                :cimi.version/parent]
                       :opt-un [:cimi.version/description]}))
