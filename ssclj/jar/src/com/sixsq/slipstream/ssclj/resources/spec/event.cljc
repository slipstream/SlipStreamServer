(ns com.sixsq.slipstream.ssclj.resources.spec.event
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.event/severity #{"critical" "high" "medium" "low"})
(s/def :cimi.event/type #{"state" "alarm" "action" "system"})

(s/def :cimi.event/state string?)
(s/def :cimi.event/resource :cimi.common/resource-link)
(s/def :cimi.event/content (su/only-keys :req-un [:cimi.event/resource
                                                  :cimi.event/state]))

(s/def :cimi/event
  (su/only-keys :req-un [:cimi.common/id
                         :cimi.common/resourceURI
                         :cimi.acl/acl

                         :cimi.core/timestamp
                         :cimi.event/content
                         :cimi.event/type
                         :cimi.event/severity]
                :opt-un [:cimi.common/created                        ;; FIXME: should be required
                         :cimi.common/updated                        ;; FIXME: should be required
                         :cimi.common/name
                         :cimi.common/description
                         :cimi.common/properties
                         :cimi.common/operations]))
