(ns com.sixsq.slipstream.ssclj.resources.spec.user-template-auto
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.user :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template :as ps]))

(s/def :cimi.user-template.auto/password :cimi.core/nonblank-string)
(s/def :cimi.user-template.auto/roles string?)
(s/def :cimi.user-template.auto/state :cimi.core/nonblank-string)
(s/def :cimi.user-template.auto/creation :cimi.core/timestamp)
(s/def :cimi.user-template.auto/lastOnline :cimi.core/timestamp)
(s/def :cimi.user-template.auto/lastExecute :cimi.core/timestamp)
(s/def :cimi.user-template.auto/activeSince :cimi.core/timestamp)
(s/def :cimi.user-template.auto/isSuperUser boolean?)
(s/def :cimi.user-template.auto/deleted boolean?)

(def user-tempate-auto-keys
  {:opt-un [:cimi.user-template.auto/password
            :cimi.user-template.auto/roles
            :cimi.user-template.auto/isSuperUser
            :cimi.user-template.auto/state
            :cimi.user-template.auto/deleted
            :cimi.user-template.auto/creation
            :cimi.user-template.auto/lastOnline
            :cimi.user-template.auto/lastExecute
            :cimi.user-template.auto/activeSince]})

(def user-template-keys-spec-req
  (su/merge-keys-specs
    [u/user-keys-spec user-tempate-auto-keys]))

(def user-template-create-keys-spec-req user-template-keys-spec-req)

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
