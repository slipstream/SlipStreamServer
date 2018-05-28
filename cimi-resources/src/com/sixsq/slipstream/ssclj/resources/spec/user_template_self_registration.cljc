(ns com.sixsq.slipstream.ssclj.resources.spec.user-template-self-registration
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.user :as user]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::password string?)
(s/def ::passwordRepeat string?)

(def user-template-self-registration-keys
  {:req-un [::user/username
            ::password
            ::passwordRepeat
            ::user/emailAddress]})

(def user-template-self-registration-keys-href
  {:opt-un [::ps/href]})

;; Defines the contents of the self-registration UserTemplate resource itself.
(s/def ::self-registration
  (su/only-keys-maps ps/resource-keys-spec
                     user-template-self-registration-keys))

;; Defines the contents of the self-registration template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :userTemplate here.
(s/def ::userTemplate
  (su/only-keys-maps ps/template-keys-spec
                     user-template-self-registration-keys
                     user-template-self-registration-keys-href))

(s/def ::self-registration-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::userTemplate]}))
