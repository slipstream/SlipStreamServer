(ns com.sixsq.slipstream.ssclj.resources.spec.module-application
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.module-component :as module-component]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [clojure.spec.alpha :as s]))


(s/def ::commit ::cimi-core/nonblank-string)
(s/def ::author ::cimi-core/nonblank-string)

(s/def ::node-name (s/and keyword? #(re-matches #"^[a-zA-Z0-9]+(_[a-zA-Z0-9]+)*$" (name %))))

(s/def ::multiplicity nat-int?)
(s/def ::maxProvisioningFailures nat-int?)

(s/def ::component ::module-component/module-link)

(s/def ::mapped boolean?)
(s/def ::value string?)

(s/def ::parameterMapping (su/only-keys :req-un [::mapped ::value]))

(s/def ::parameterMappings (s/map-of ::module-component/parameter-keyword ::parameterMapping))

(s/def ::node (su/only-keys :req-un [::multiplicity ::component]
                            :opt-un [::maxProvisioningFailures
                                     ::parameterMappings]))

(s/def ::nodes (s/map-of ::node-name ::node))

(def module-application-keys-spec (su/merge-keys-specs [c/common-attrs
                                                        {:req-un [::nodes
                                                                  ::author]
                                                         :opt-un [::commit]}]))

(s/def ::module-application (su/only-keys-maps module-application-keys-spec))
