(ns com.sixsq.slipstream.ssclj.resources.spec.module-image
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::os #{"CentOS" "Debian" "Fedora" "OpenSuSE" "RedHat" "SLES" "Ubuntu" "Windows" "Other"})

(def connector-href-regex #"^connector/[a-z]+(-[a-z]+)*$")

(s/def ::loginUser ::cimi-core/nonblank-string)
(s/def ::connectorClass (s/and string? #(re-matches connector-href-regex %)))
(s/def ::connector (s/and string? #(re-matches connector-href-regex %)))

(def module-href-regex #"^module/[a-z]+(-[a-z]+)*$")

(s/def ::relatedImage (s/and string? #(re-matches module-href-regex %)))


(def module-image-keys-spec (su/merge-keys-specs [c/common-attrs
                                                  {:req-un [::os ::loginUser ::connector]
                                                   :opt-un [::connectorClass ::relatedImage]}]))

(s/def ::module-image (su/only-keys-maps module-image-keys-spec))
