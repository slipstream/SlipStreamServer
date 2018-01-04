(ns com.sixsq.slipstream.ssclj.resources.spec.user-auto
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.user :as ps]
    [clojure.spec.alpha :as s]))

(s/def :cimi.user-auto/method :cimi.core/identifier)
(s/def :cimi.user-auto/href string?)
(s/def :cimi.user-auto/password :cimi.core/nonblank-string)
(s/def :cimi.user-auto/roles string?)
(s/def :cimi.user-auto/state :cimi.core/nonblank-string)
(s/def :cimi.user-auto/creation :cimi.core/timestamp)
(s/def :cimi.user-auto/lastOnline :cimi.core/timestamp)
(s/def :cimi.user-auto/lastExecute :cimi.core/timestamp)
(s/def :cimi.user-auto/activeSince :cimi.core/timestamp)
(s/def :cimi.user-auto/isSuperUser boolean?)
(s/def :cimi.user-auto/deleted boolean?)
(s/def :cimi.user-auto/githublogin string?)
(s/def :cimi.user-auto/cyclonelogin string?)

(def user-auto-keys-spec
  {:opt-un [:cimi.user-auto/method
            :cimi.user-auto/href
            :cimi.user-auto/password
            :cimi.user-auto/roles
            :cimi.user-auto/isSuperUser
            :cimi.user-auto/state
            :cimi.user-auto/deleted
            :cimi.user-auto/creation
            :cimi.user-auto/lastOnline
            :cimi.user-auto/lastExecute
            :cimi.user-auto/activeSince
            :cimi.user-auto/githublogin
            :cimi.user-auto/cyclonelogin]})

(s/def :cimi/user-auto
  (su/only-keys-maps
    ps/user-common-attrs
    ps/user-keys-spec
    user-auto-keys-spec))
