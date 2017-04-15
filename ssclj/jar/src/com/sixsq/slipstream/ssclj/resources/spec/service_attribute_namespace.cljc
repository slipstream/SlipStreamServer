(ns com.sixsq.slipstream.ssclj.resources.spec.service-attribute-namespace
  (:require
    [clojure.spec :as s]
    [clojure.spec.gen :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(def prefix-regex #"^[a-z]([a-z0-9-]*[a-z0-9])?$")
(def char-prefix (gen/fmap char (s/gen (set (concat (range 97 123) (range 48 58) [45])))))
(def gen-prefix (gen/fmap str/join (gen/vector char-prefix)))
(s/def :cimi.service-attribute-namespace/prefix (s/with-gen (s/and string? #(re-matches prefix-regex %))
                                                            (constantly gen-prefix)))

(s/def :cimi/service-attribute-namespace
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.acl/acl]}
                     {:req-un [:cimi.service-attribute-namespace/prefix
                               :cimi.core/uri]}))
