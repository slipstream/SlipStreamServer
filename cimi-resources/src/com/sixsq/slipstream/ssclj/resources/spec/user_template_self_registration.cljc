(ns com.sixsq.slipstream.ssclj.resources.spec.user-template-self-registration
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.user :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def :cimi.user-template.self-registration/href :cimi.user-template/href)

(s/def :cimi.user-template.self-registration/username :cimi.user/username)
(s/def :cimi.user-template.self-registration/password string?)
(s/def :cimi.user-template.self-registration/passwordRepeat string?)
(s/def :cimi.user-template.self-registration/emailAddress string?)

(def user-template-self-registration-keys
  {:req-un [:cimi.user-template.self-registration/username
            :cimi.user-template.self-registration/password
            :cimi.user-template.self-registration/passwordRepeat
            :cimi.user-template.self-registration/emailAddress]})

(def user-template-self-registration-keys-href
  {:opt-un [:cimi.user-template.self-registration/href]})

;; Defines the contents of the self-registration UserTemplate resource itself.
(s/def :cimi/user-template.self-registration
  (su/only-keys-maps ps/resource-keys-spec
                     user-template-self-registration-keys))

;; Defines the contents of the self-registration template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :userTemplate here.
(s/def :cimi.user-template.self-registration/userTemplate
  (su/only-keys-maps ps/template-keys-spec
                     user-template-self-registration-keys
                     user-template-self-registration-keys-href))

(s/def :cimi/user-template.self-registration-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.user-template.self-registration/userTemplate]}))
