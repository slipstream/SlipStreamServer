(ns com.sixsq.slipstream.ssclj.resources.spec.module-image
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::os #{"CentOS" "Debian" "Fedora" "OpenSuSE" "RedHat" "SLES" "Ubuntu" "Windows" "Other"})

(def connector-href-regex #"^connector/[a-z]+(-[a-z]+)*$")
(s/def ::connector-href (s/and string? #(re-matches connector-href-regex %)))

(s/def ::loginUser ::cimi-core/nonblank-string)
(s/def ::sudo boolean?)
(s/def ::connectorClass ::connector-href)

(s/def :connector/href ::connector-href)
(s/def ::connector (s/keys :req-un [:connector/href]))

(def module-href-regex #"^module/[a-z]+(-[a-z]+)*$")

(s/def ::href (s/and string? #(re-matches module-href-regex %)))
(s/def ::relatedImage (s/keys :req-un [::href]))


(def module-image-keys-spec (su/merge-keys-specs [c/common-attrs
                                                  {:req-un [::os ::loginUser ::connector]
                                                   :opt-un [::sudo ::connectorClass ::relatedImage]}]))

(s/def ::module-image (su/only-keys-maps module-image-keys-spec))
