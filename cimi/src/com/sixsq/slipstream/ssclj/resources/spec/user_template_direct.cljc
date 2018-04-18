(ns com.sixsq.slipstream.ssclj.resources.spec.user-template-direct
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.user :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def :cimi.user-template.direct/href :cimi.user-template/href)
(s/def :cimi.user-template.direct/password ::cimi-core/nonblank-string)
(s/def :cimi.user-template.direct/roles string?)
(s/def :cimi.user-template.direct/state ::cimi-core/nonblank-string)
(s/def :cimi.user-template.direct/creation ::cimi-core/timestamp)
(s/def :cimi.user-template.direct/lastOnline ::cimi-core/timestamp)
(s/def :cimi.user-template.direct/lastExecute ::cimi-core/timestamp)
(s/def :cimi.user-template.direct/activeSince ::cimi-core/timestamp)
(s/def :cimi.user-template.direct/isSuperUser boolean?)
(s/def :cimi.user-template.direct/deleted boolean?)
(s/def :cimi.user-template.direct/githublogin string?)
(s/def :cimi.user-template.direct/cyclonelogin string?)

(def user-tempate-direct-keys
  {:opt-un [:cimi.user-template.direct/href
            :cimi.user-template.direct/password
            :cimi.user-template.direct/roles
            :cimi.user-template.direct/isSuperUser
            :cimi.user-template.direct/state
            :cimi.user-template.direct/deleted
            :cimi.user-template.direct/creation
            :cimi.user-template.direct/lastOnline
            :cimi.user-template.direct/lastExecute
            :cimi.user-template.direct/activeSince
            :cimi.user-template.direct/githublogin
            :cimi.user-template.direct/cyclonelogin]})

(def user-template-keys-spec-req
  (su/merge-keys-specs
    [u/user-keys-spec user-tempate-direct-keys]))

(def user-template-create-keys-spec-req user-template-keys-spec-req)

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
