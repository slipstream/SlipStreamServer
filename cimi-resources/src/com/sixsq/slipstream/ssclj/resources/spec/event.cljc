(ns com.sixsq.slipstream.ssclj.resources.spec.event
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def ::severity #{"critical" "high" "medium" "low"})
(s/def ::type #{"state" "alarm" "action" "system"})

;; Events may need to reference resources that do not follow the CIMI.
;; conventions.  Allow for a more flexible schema to be used here.
(s/def ::href (s/and string? #(re-matches #"^[a-zA-Z0-9]+[a-zA-Z0-9_./-]*$" %)))
(s/def ::resource-link (s/keys :req-un [::href]))

(s/def ::state string?)
(s/def ::resource ::resource-link)
(s/def ::content (su/only-keys :req-un [::resource ::state]))

(s/def ::event
  (su/only-keys :req-un [::cimi-common/id
                         ::cimi-common/resourceURI
                         ::cimi-common/acl

                         ::cimi-core/timestamp
                         ::content
                         ::type
                         ::severity]
                :opt-un [::cimi-common/created              ;; FIXME: should be required
                         ::cimi-common/updated              ;; FIXME: should be required
                         ::cimi-common/name
                         ::cimi-common/description
                         ::cimi-common/properties
                         ::cimi-common/operations]))
