(ns com.sixsq.slipstream.ssclj.resources.spec.module
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))


(s/def :cimi.module/parent string?)
(s/def :cimi.module/path :cimi.core/nonblank-string)
(s/def :cimi.module/type #{"project" "image" "component" "application"})

(s/def :cimi.module/versions :cimi.common/resource-links)


(def common-module-attrs {:req-un [:cimi.common/name        ;; normally optional, force it to be required
                                   :cimi.module/parent
                                   :cimi.module/path
                                   :cimi.module/type]})


;; FIXME: Does this need to be extensible?  I.e. is the [keyword? any?] really needed?
(s/def :cimi/module
  (su/constrained-map keyword? any?
                      c/common-attrs
                      common-module-attrs
                      {:req-un [:cimi.module/versions]}))
