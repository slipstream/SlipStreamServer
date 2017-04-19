(ns com.sixsq.slipstream.ssclj.resources.spec.user-template-direct
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template :as ps]))

(s/def :cimi.user-template.direct/username :cimi.core/nonblank-string)
(s/def :cimi.user-template.direct/emailAddress :cimi.core/nonblank-string)
(s/def :cimi.user-template.direct/firstName :cimi.core/nonblank-string)
(s/def :cimi.user-template.direct/lastName :cimi.core/nonblank-string)
(s/def :cimi.user-template.direct/organization :cimi.core/nonblank-string)

(def user-template-keys-spec-req
  {:req-un [:cimi.user-template.direct/username
            :cimi.user-template.direct/emailAddress]
   :opt-un [:cimi.user-template.direct/firstName
            :cimi.user-template.direct/lastName
            :cimi.user-template.direct/organization]})

(def user-template-create-keys-spec-req
  {:req-un [:cimi.user-template.direct/username
            :cimi.user-template.direct/emailAddress]
   :opt-un [:cimi.user-template.direct/firstName
            :cimi.user-template.direct/lastName
            :cimi.user-template.direct/organization]})

;; Defines the contents of the direct UserTemplate resource itself.
(s/def :cimi/user-template.direct
  (su/only-keys-maps ps/resource-keys-spec
                     user-template-keys-spec-req))

;; Defines the contents of the direct template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :userTemplate here.
(s/def :cimi.user-template.direct/userTemplate
  (su/only-keys-maps ps/template-keys-spec
                     user-template-create-keys-spec-req))

(s/def :cimi/user-template.direct-create
  (su/only-keys-maps ps/create-keys-spec
                     {:opt-un [:cimi.user-template.direct/userTemplate]}))
