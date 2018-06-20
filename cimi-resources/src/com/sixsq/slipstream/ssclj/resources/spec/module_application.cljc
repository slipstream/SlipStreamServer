(ns com.sixsq.slipstream.ssclj.resources.spec.module-application
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.virtual-machine :as vm]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::node-name (s/and keyword? #(re-matches #"^[a-z0-9]+(_[a-z0-9]+)*$" (name %))))

(s/def ::multiplicity nat-int?)
(s/def ::maxProvisioningFailures nat-int?)

(def module-href-regex #"^module/[a-z]+(-[a-z]+)*$")
(s/def ::href (s/and string? #(re-matches module-href-regex %)))
(s/def ::component (s/keys :req-un [::href]))

(s/def ::parameter-name (s/and keyword? #(re-matches #"^[a-z0-9]+([\.-][a-z0-9]+)*$" (name %))))

(s/def ::mapped boolean?)
(s/def ::value string?)

(s/def ::parameterMapping (su/only-keys :req-un [::mapped ::value]))

(s/def ::parameterMappings (s/map-of ::parameter-name ::parameterMapping))

(s/def ::node (su/only-keys :req-un [::multiplicity ::component]
                            :opt-un [::maxProvisioningFailures
                                     ::parameterMappings]))

(s/def ::nodes (s/map-of  ::node-name ::node))

(def module-application-keys-spec (su/merge-keys-specs [c/common-attrs
                                                        {:req-un [::nodes]}]))

(s/def ::module-application (su/only-keys-maps module-application-keys-spec))
