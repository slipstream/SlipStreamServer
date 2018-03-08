(ns com.sixsq.slipstream.ssclj.resources.spec.user-self-registration
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.user :as ps]
    [clojure.spec.alpha :as s]))

(s/def :cimi.user-self-registration/method :cimi.core/identifier)
(s/def :cimi.user-self-registration/href string?)
(s/def :cimi.user-self-registration/password :cimi.core/nonblank-string)
(s/def :cimi.user-self-registration/roles string?)
(s/def :cimi.user-self-registration/state :cimi.core/nonblank-string)
(s/def :cimi.user-self-registration/creation :cimi.core/timestamp)
(s/def :cimi.user-self-registration/lastOnline :cimi.core/timestamp)
(s/def :cimi.user-self-registration/lastExecute :cimi.core/timestamp)
(s/def :cimi.user-self-registration/activeSince :cimi.core/timestamp)
(s/def :cimi.user-self-registration/isSuperUser boolean?)
(s/def :cimi.user-self-registration/deleted boolean?)
(s/def :cimi.user-self-registration/githublogin string?)
(s/def :cimi.user-self-registration/cyclonelogin string?)

(def user-self-registration-keys-spec
  {:opt-un [:cimi.user-self-registration/method
            :cimi.user-self-registration/href
            :cimi.user-self-registration/password
            :cimi.user-self-registration/roles
            :cimi.user-self-registration/isSuperUser
            :cimi.user-self-registration/state
            :cimi.user-self-registration/deleted
            :cimi.user-self-registration/creation
            :cimi.user-self-registration/lastOnline
            :cimi.user-self-registration/lastExecute
            :cimi.user-self-registration/activeSince
            :cimi.user-self-registration/githublogin
            :cimi.user-self-registration/cyclonelogin]})

(s/def :cimi/user-self-registration
  (su/only-keys-maps
    ps/user-common-attrs
    ps/user-keys-spec
    user-self-registration-keys-spec))
