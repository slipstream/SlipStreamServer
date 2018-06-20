(ns com.sixsq.slipstream.ssclj.resources.spec.module-versions
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::path (s/and string? #(re-matches #"^[a-zA-Z][\w\.-]*(/[a-zA-Z][\w\.-]*)*$" %)))

(s/def ::type #{"IMAGE" "COMPONENT" "APPLICATION"})

(s/def ::versions ::cimi-common/resource-links)

(s/def ::logo ::cimi-common/resource-link)


(def module-keys-spec (su/merge-keys-specs [c/common-attrs
                                            {:req-un [::path ::type ::versions]
                                             :opt-un [::logo]}]))

(s/def ::module-versions (su/only-keys-maps module-keys-spec))
