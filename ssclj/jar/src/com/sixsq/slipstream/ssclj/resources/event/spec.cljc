(ns com.sixsq.slipstream.ssclj.resources.event.spec
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.common.spec :as c]))

(s/def ::severity #{"critical" "high" "medium" "low"})
(s/def ::type #{"state" "alarm" "action" "system"})

(s/def ::state string?)
(s/def ::resource ::c/resource-link)
(s/def ::content (s/keys :req-un [::resource ::state]))

(s/def ::event (c/only-keys :req-un [::c/id
                                     ::c/resourceURI
                                     ::c/acl

                                     ::timestamp
                                     ::content
                                     ::type
                                     ::severity]
                            :opt-un [::c/created            ;; FIXME: should be required
                                     ::c/updated            ;; FIXME: should be required
                                     ::c/name
                                     ::c/description
                                     ::c/properties
                                     ::c/operations]))
