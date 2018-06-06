(ns com.sixsq.slipstream.ssclj.resources.spec.user-template-oidc-registration
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.user :as user]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

#_(def user-template-oidc-registration-keys nil)

(def user-template-oidc-registration-keys-href
  {:opt-un [::ps/href]})

;; Defines the contents of the oidc-registration UserTemplate resource itself.
(s/def ::oidc-registration
  (su/only-keys-maps ps/resource-keys-spec
                     ;;user-template-oidc-registration-keys
                     ))

;; Defines the contents of the oidc-registration template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :userTemplate here.
(s/def ::userTemplate
  (su/only-keys-maps ps/template-keys-spec
                     ;;user-template-oidc-registration-keys
                     user-template-oidc-registration-keys-href))

(s/def ::oidc-registration-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::userTemplate]}))
