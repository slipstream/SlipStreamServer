(ns com.sixsq.slipstream.ssclj.resources.spec.event
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.event/severity #{"critical" "high" "medium" "low"})
(s/def :cimi.event/type #{"state" "alarm" "action" "system"})

;; Events may need to reference resources that do not follow the CIMI.
;; conventions.  Allow for a more flexible schema to be used here.
(s/def :cimi.event.link/href
  (su/regex-string #"[a-zA-Z0-9_./-]" #"^[a-zA-Z0-9]+[a-zA-Z0-9_./-]*$"))
(s/def :cimi.event/resource-link (s/keys :req-un [:cimi.event.link/href]))

(s/def :cimi.event/state string?)
(s/def :cimi.event/resource :cimi.event/resource-link)
(s/def :cimi.event/content (su/only-keys :req-un [:cimi.event/resource
                                                  :cimi.event/state]))

(s/def :cimi/event
  (su/only-keys :req-un [:cimi.common/id
                         :cimi.common/resourceURI
                         :cimi.common/acl

                         :cimi.core/timestamp
                         :cimi.event/content
                         :cimi.event/type
                         :cimi.event/severity]
                :opt-un [:cimi.common/created               ;; FIXME: should be required
                         :cimi.common/updated               ;; FIXME: should be required
                         :cimi.common/name
                         :cimi.common/description
                         :cimi.common/properties
                         :cimi.common/operations]))
