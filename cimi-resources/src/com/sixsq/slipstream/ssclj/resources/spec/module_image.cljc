(ns com.sixsq.slipstream.ssclj.resources.spec.module-image
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::cpu pos-int?)
(s/def ::ram pos-int?)
(s/def ::disk pos-int?)
(s/def ::volatileDisk pos-int?)
(s/def ::networkType #{"public" "private"})

(s/def ::os #{"CentOS" "Debian" "Fedora" "OpenSuSE" "RedHat" "SLES" "Ubuntu" "Windows" "Other"})

(def connector-href-regex #"^connector.*/[a-z]+(-[a-z]+)*$")
(s/def ::connector-href (s/and string? #(re-matches connector-href-regex %)))

(s/def ::loginUser ::cimi-core/nonblank-string)
(s/def ::sudo boolean?)

(s/def ::connectorClass (s/keys :req-un [:connector/href
                                         ::imageID]))

(s/def ::connectorClasses (s/nilable (s/coll-of ::connectorClass :min-count 1 :kind vector?)))

(s/def ::imageID ::cimi-core/nonblank-string)

(s/def :connector/href ::connector-href)
(s/def ::connector (s/keys :req-un [:connector/href
                                    ::imageID]))

(s/def ::connectors (s/nilable (s/coll-of ::connector :min-count 1 :kind vector?)))

(def module-href-regex #"^module/[a-z]+(-[a-z]+)*$")

(s/def ::href (s/and string? #(re-matches module-href-regex %)))
(s/def ::relatedImage (s/keys :req-un [::href]))


(def module-image-keys-spec (su/merge-keys-specs [c/common-attrs
                                                  {:req-un [::os ::loginUser ::networkType]
                                                   :opt-un [::connectors ::connectorClasses
                                                            ::sudo ::relatedImage
                                                            ::cpu ::ram ::disk ::volatileDisk]}]))

(s/def ::module-image (su/only-keys-maps module-image-keys-spec))
