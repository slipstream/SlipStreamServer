(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

;; All external object resources must have a 'type' attribute.
(s/def :cimi.external-object-template/objectType :cimi.core/identifier)

;; External object resources may have a 'state' attribute e.g to know
;; if the above points to an existing object
(s/def :cimi.external-object-template/state #{"new" "ready"})


;; Restrict the href used to create external objects.
(def external-object-template-regex #"^external-object-template/[a-z]+(-[a-z]+)*$")
(s/def :cimi.external-object-template/href (s/and string? #(re-matches external-object-template-regex %)))

(s/def :cimi.external-object-template/contentType :cimi.core/nonblank-string)

(s/def :cimi.external-object-template/filename :cimi.core/nonblank-string)

;;
;; Keys specifications for ExternalObjectTemplate resources.

(def external-object-template-keys-spec {:req-un [:cimi.external-object-template/objectType
                                                  :cimi.external-object-template/state]
                                         :opt-un [:cimi.external-object-template/href
                                                  :cimi.external-object-template/contentType
                                                  :cimi.external-object-template/filename]})
(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        external-object-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        external-object-template-keys-spec]))

