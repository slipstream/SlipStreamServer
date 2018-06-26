(ns com.sixsq.slipstream.ssclj.resources.spec.module-component
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(def module-href-regex #"^module/[a-z0-9]+(-[a-z0-9]+)*$")
(s/def ::href (s/and string? #(re-matches module-href-regex %)))
(s/def ::parent (s/keys :req-un [::href]))

(s/def ::cpu pos-int?)
(s/def ::ram pos-int?)
(s/def ::disk pos-int?)
(s/def ::volatileDisk pos-int?)
(s/def ::networkType #{"public" "private"})

(s/def ::name (s/and keyword? #(re-matches #"^[a-zA-Z0-9]+([_\.-][a-zA-Z0-9]+)*$" (name %))))
(s/def ::description ::cimi-core/nonblank-string)
(s/def ::value ::cimi-core/nonblank-string)

(s/def ::parameterMap (su/only-keys :opt-un [::description ::value]))

(s/def ::inputParameters (s/map-of ::name ::parameterMap))
(s/def ::outputParameters (s/map-of ::name ::parameterMap))

(s/def ::target ::cimi-core/nonblank-string)
(s/def ::package-list (s/coll-of ::cimi-core/nonblank-string :min-count 1 :kind vector?))

(s/def ::preinstall ::target)
(s/def ::packages ::package-list)
(s/def ::postinstall ::target)
(s/def ::deployment ::target)
(s/def ::reporting ::target)
(s/def ::onVmAdd ::target)
(s/def ::onVmRemove ::target)
(s/def ::prescale ::target)
(s/def ::postscale ::target)

(s/def ::targets (su/only-keys :opt-un [::preinstall ::packages
                                        ::postinstall ::deployment
                                        ::reporting ::onVmAdd
                                        ::onVmRemove ::prescale
                                        ::postscale]))


(def module-component-keys-spec (su/merge-keys-specs [c/common-attrs
                                                      {:req-un [::parent ::networkType ::outputParameters]
                                                       :opt-un [::inputParameters ::cpu ::ram ::disk ::volatileDisk ::targets]}]))

(s/def ::module-component (su/only-keys-maps module-component-keys-spec))
