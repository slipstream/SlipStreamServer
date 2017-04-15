(ns com.sixsq.slipstream.ssclj.resources.spec.service-attribute
  (:require
    [clojure.spec :as s]
    [clojure.spec.gen :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(def prefix-regex #"^[a-z]([a-z0-9-]*[a-z0-9])?$")
(def char-prefix (gen/fmap char (s/gen (set (concat (range 97 123) (range 48 58) [45])))))
(def gen-prefix (gen/fmap str/join (gen/vector char-prefix)))
(s/def :cimi.service-attribute/prefix (s/with-gen (s/and string? #(re-matches prefix-regex %))
                                                  (constantly gen-prefix)))

(s/def :cimi.service-attribute/attributeName :cimi.core/nonblank-string)

(s/def :cimi.service-attribute/type :cimi.core/nonblank-string)

(s/def :cimi/service-attribute
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.common/name            ;; name is required
                               :cimi.common/description     ;; description is required
                               :cimi.service-attribute/prefix
                               :cimi.service-attribute/attributeName
                               :cimi.service-attribute/type]}))
