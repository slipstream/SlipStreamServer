(ns com.sixsq.slipstream.ssclj.resources.spec.user-direct
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.user :as ps]
    [clojure.spec.alpha :as s]))

(s/def :cimi.user-direct/method :cimi.core/identifier)
(s/def :cimi.user-direct/href string?)
(s/def :cimi.user-direct/password :cimi.core/nonblank-string)
(s/def :cimi.user-direct/roles string?)
(s/def :cimi.user-direct/state :cimi.core/nonblank-string)
(s/def :cimi.user-direct/creation :cimi.core/timestamp)
(s/def :cimi.user-direct/lastOnline :cimi.core/timestamp)
(s/def :cimi.user-direct/lastExecute :cimi.core/timestamp)
(s/def :cimi.user-direct/activeSince :cimi.core/timestamp)
(s/def :cimi.user-direct/isSuperUser boolean?)
(s/def :cimi.user-direct/deleted boolean?)
(s/def :cimi.user-direct/githublogin string?)
(s/def :cimi.user-direct/cyclonelogin string?)

(def user-direct-keys-spec
  {:opt-un [:cimi.user-direct/method
            :cimi.user-direct/href
            :cimi.user-direct/password
            :cimi.user-direct/roles
            :cimi.user-direct/isSuperUser
            :cimi.user-direct/state
            :cimi.user-direct/deleted
            :cimi.user-direct/creation
            :cimi.user-direct/lastOnline
            :cimi.user-direct/lastExecute
            :cimi.user-direct/activeSince
            :cimi.user-direct/githublogin
            :cimi.user-direct/cyclonelogin]})

(s/def :cimi/user-direct
  (su/only-keys-maps
    ps/user-common-attrs
    ps/user-keys-spec
    user-direct-keys-spec))
