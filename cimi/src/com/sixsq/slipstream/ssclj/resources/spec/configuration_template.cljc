(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;; Parameter definitions.

(s/def ::service ::cimi-core/identifier)
(s/def ::instance ::cimi-core/identifier)

(def configuration-template-regex #"^configuration-template/[a-z0-9]+(-[a-z0-9]+)*$")
(s/def ::href (s/and string? #(re-matches configuration-template-regex %)))

(s/def ::configurationTemplate (su/only-keys-maps
                                 {:req-un [::href]}))

;;
;; Keys specifications for configuration-template resources.
;; As this is a "base class" for configuration-template resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def configuration-template-keys-spec {:req-un [::service]
                                       :opt-un [::instance ::configurationTemplate]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs configuration-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        configuration-template-keys-spec
                        {:opt-un [::href]}]))

