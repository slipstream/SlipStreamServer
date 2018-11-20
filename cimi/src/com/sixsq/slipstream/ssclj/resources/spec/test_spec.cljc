(ns com.sixsq.slipstream.ssclj.resources.spec.test-spec
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


;; Less restrictive than standard ::cimi-common/id to accommodate OIDC, etc.
(s/def ::userid (s/and string? #(re-matches #"^user/.*" %)))

(s/def ::href ::userid)
(s/def ::resource-link (s/keys :req-un [::href]))

(s/def ::a
  (-> (st/spec ::resource-link)
      (assoc :name "user")))

(s/def ::b
  (-> (st/spec ::resource-link)
      (assoc :name "user")))

(s/def ::c
  (-> (st/spec (su/only-keys :req-un [::a] :opt-un [::b]))))

(s/def ::d
  (-> (st/spec (su/only-keys-maps {:req-un [::a] :opt-un [::b]}))))

(s/def ::resource
  (s/keys :req-un [::a ::c]
          :opt-un [::b]))
