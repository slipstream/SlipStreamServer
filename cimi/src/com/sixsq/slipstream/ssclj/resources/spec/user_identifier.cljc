(ns com.sixsq.slipstream.ssclj.resources.spec.user-identifier
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::identifier string?)

;; Less restrictive than standard ::cimi-common/id to accommodate OIDC, etc.
(s/def ::userid (s/and string? #(re-matches #"^user/.*" %)))

(s/def ::href ::userid)
(s/def ::resource-link (s/keys :req-un [::href]))

(s/def ::user ::resource-link)

(s/def ::user-identifier
  (su/only-keys-maps c/common-attrs
                     {:req-un [::identifier ::user]}))
