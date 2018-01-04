(ns com.sixsq.slipstream.ssclj.resources.spec.user
  (:require
    [clojure.string :as str]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.user/username :cimi.core/nonblank-string)
(s/def :cimi.user/emailAddress :cimi.core/nonblank-string)
(s/def :cimi.user/firstName :cimi.core/nonblank-string)
(s/def :cimi.user/lastName :cimi.core/nonblank-string)
(s/def :cimi.user/organization string?)

(s/def :cimi.user/id (s/and string? #(re-matches #"^user/.*" %)))

(def user-keys-spec
  {:req-un [:cimi.user/username
            :cimi.user/emailAddress]
   :opt-un [:cimi.user/firstName
            :cimi.user/lastName
            :cimi.user/organization]})

; substitute :cimi.common/id with less strict cimi.user/id
#_(def user-common-attrs
    (->> c/common-attrs
         :req-un
         (remove #{:cimi.common/id})
         (concat [:cimi.user/id])
         (hash-map :req-un)
         (merge c/common-attrs)))
; FIXME: fix the above def and remove this copy/paste
(def ^:const user-common-attrs
  {:req-un [:cimi.user/id
            :cimi.common/resourceURI
            :cimi.common/created
            :cimi.common/updated
            :cimi.common/acl]
   :opt-un [:cimi.common/name
            :cimi.common/description
            :cimi.common/properties
            :cimi.common/operations]})

(s/def :cimi/user
  (su/only-keys-maps user-common-attrs
                     user-keys-spec))
