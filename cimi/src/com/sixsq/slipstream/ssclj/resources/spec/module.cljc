(ns com.sixsq.slipstream.ssclj.resources.spec.module
  (:require
    [clojure.spec.alpha :as s]
    [instaparse.core :as insta]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]))


(s/def :cimi.module/name :cimi.common/name)
(s/def :cimi.module/path :cimi.core/nonblank-string)
(s/def :cimi.module/parent string?)
(s/def :cimi.module/description string?)
(s/def :cimi.module/type #{"project" "component" "application"})
(s/def :cimi.module/versions :cimi.common/resource-links )

(s/def :cimi/module
  (su/constrained-map keyword? any?
                       c/common-attrs
                       {:req-un [:cimi.module/name
                                 :cimi.module/path
                                 :cimi.module/type
                                 :cimi.module/parent
                                 :cimi.module/versions]
                        :opt-un [:cimi.module/description]}))
