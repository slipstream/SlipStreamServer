(ns com.sixsq.slipstream.ssclj.resources.spec.user
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


;; Less restrictive than standard ::cimi-common/id to accommodate OIDC, etc.
(s/def ::id (s/and string? #(re-matches #"^user/.*" %)))

(s/def ::username ::cimi-core/nonblank-string)
(s/def ::emailAddress ::cimi-core/nonblank-string)

(s/def ::firstName ::cimi-core/nonblank-string)
(s/def ::lastName ::cimi-core/nonblank-string)
(s/def ::organization string?)

(s/def ::method ::cimi-core/identifier)
(s/def ::href string?)
(s/def ::password ::cimi-core/nonblank-string)
(s/def ::roles string?)
(s/def ::state #{"NEW" "ACTIVE" "DELETED" "SUSPENDED"})
(s/def ::creation ::cimi-core/timestamp)
(s/def ::lastOnline ::cimi-core/timestamp)
(s/def ::lastExecute ::cimi-core/timestamp)
(s/def ::activeSince ::cimi-core/timestamp)
(s/def ::isSuperUser boolean?)
(s/def ::deleted boolean?)
(s/def ::githublogin string?)
(s/def ::cyclonelogin string?)                              ;; Deprecated and unused.  Kept for backward compatibility.


;;
;; redefined common attributes to allow for less restrictive
;; resource identifier (::id) for user resources
;;

(def ^:const user-common-attrs
  {:req-un [::id
            ::cimi-common/resourceURI
            ::cimi-common/created
            ::cimi-common/updated
            ::cimi-common/acl]
   :opt-un [::cimi-common/name
            ::cimi-common/description
            ::cimi-common/properties
            ::cimi-common/operations]})


(def user-keys-spec
  {:req-un [::username
            ::emailAddress]
   :opt-un [::firstName
            ::lastName
            ::organization

            ::method
            ::href
            ::password
            ::roles
            ::isSuperUser
            ::state
            ::deleted
            ::creation
            ::lastOnline
            ::lastExecute
            ::activeSince
            ::githublogin
            ::cyclonelogin]})


(s/def :cimi/user
  (su/only-keys-maps user-common-attrs
                     user-keys-spec))
