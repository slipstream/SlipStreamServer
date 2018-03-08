(ns com.sixsq.slipstream.ssclj.resources.spec.user
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def :cimi.user/username :cimi.core/nonblank-string)
(s/def :cimi.user/emailAddress :cimi.core/nonblank-string)
(s/def :cimi.user/firstName :cimi.core/nonblank-string)
(s/def :cimi.user/lastName :cimi.core/nonblank-string)
(s/def :cimi.user/organization string?)

(s/def :cimi.user/id (s/and string? #(re-matches #"^user/.*" %)))

(s/def :cimi.user/method :cimi.core/identifier)
(s/def :cimi.user/href string?)
(s/def :cimi.user/password :cimi.core/nonblank-string)
(s/def :cimi.user/roles string?)
(s/def :cimi.user/state #{"NEW" "ACTIVE" "DELETED"})
(s/def :cimi.user/creation :cimi.core/timestamp)
(s/def :cimi.user/lastOnline :cimi.core/timestamp)
(s/def :cimi.user/lastExecute :cimi.core/timestamp)
(s/def :cimi.user/activeSince :cimi.core/timestamp)
(s/def :cimi.user/isSuperUser boolean?)
(s/def :cimi.user/deleted boolean?)
(s/def :cimi.user/githublogin string?)
(s/def :cimi.user/cyclonelogin string?)


(def ^:const user-common-attrs
  {:req-un [:cimi.user/id                                   ;; less restrictive than :cimi.common/id
            :cimi.common/resourceURI
            :cimi.common/created
            :cimi.common/updated
            :cimi.common/acl]
   :opt-un [:cimi.common/name
            :cimi.common/description
            :cimi.common/properties
            :cimi.common/operations]})


(def user-keys-spec
  {:req-un [:cimi.user/username
            :cimi.user/emailAddress]
   :opt-un [:cimi.user/firstName
            :cimi.user/lastName
            :cimi.user/organization

            :cimi.user/method
            :cimi.user/href
            :cimi.user/password
            :cimi.user/roles
            :cimi.user/isSuperUser
            :cimi.user/state
            :cimi.user/deleted
            :cimi.user/creation
            :cimi.user/lastOnline
            :cimi.user/lastExecute
            :cimi.user/activeSince
            :cimi.user/githublogin
            :cimi.user/cyclonelogin]})


(s/def :cimi/user
  (su/only-keys-maps user-common-attrs
                     user-keys-spec))
