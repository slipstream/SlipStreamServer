(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;; All credential templates must indicate the type of credential to create.
(s/def :cimi.credential-template/type ::cimi-core/identifier)

;; A given credential may have more than one method for creating it.  All
;; credential templates must provide a method name.
(s/def :cimi.credential-template/method ::cimi-core/identifier)

(s/def :cimi.credential-template/enabled boolean?)

(def credential-template-regex #"^credential-template/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$")
(s/def :cimi.credential-template/href (s/and string? #(re-matches credential-template-regex %)))

;;
;; Keys specifications for CredentialTemplate resources.
;; As this is a "base class" for CredentialTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def credential-template-keys-spec {:req-un [:cimi.credential-template/type
                                             :cimi.credential-template/method]
                                    :opt-un [:cimi.credential-template/enabled]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        credential-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

;; subclasses MUST provide the href to the template to use
(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        credential-template-keys-spec]))

