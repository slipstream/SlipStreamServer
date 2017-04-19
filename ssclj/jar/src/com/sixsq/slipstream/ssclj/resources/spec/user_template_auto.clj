(ns com.sixsq.slipstream.ssclj.resources.spec.user-template-auto
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template :as ps]))

(s/def :cimi.user-template.auto/username :cimi.core/nonblank-string)
(s/def :cimi.user-template.auto/emailAddress :cimi.core/nonblank-string)

(def user-template-keys-spec-req
  {:req-un [:cimi.user-template.auto/username
            :cimi.user-template.auto/emailAddress]})

(def user-template-create-keys-spec-req
  {:req-un [:cimi.user-template.auto/username
            :cimi.user-template.auto/emailAddress]})

;; Defines the contents of the auto UserTemplate resource itself.
(s/def :cimi/user-template.auto
  (su/only-keys-maps ps/resource-keys-spec
                     user-template-keys-spec-req))

;; Defines the contents of the auto template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :userTemplate here.
(s/def :cimi.user-template.auto/userTemplate
  (su/only-keys-maps ps/template-keys-spec
                     user-template-create-keys-spec-req))

(s/def :cimi/user-template.auto-create
  (su/only-keys-maps ps/create-keys-spec
                     {:opt-un [:cimi.user-template.auto/userTemplate]}))
