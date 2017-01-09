(ns com.sixsq.slipstream.ssclj.resources.usage-event.spec
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.common.spec :as c]))

(s/def ::cloud-vm-instanceid ::c/nonblank-string)
(s/def ::user ::c/nonblank-string)
(s/def ::cloud ::c/nonblank-string)
(s/def ::start-timestamp ::c/timestamp)
(s/def ::end-timestamp ::c/optional-timestamp)

(s/def ::name ::c/nonblank-string)
(s/def ::value ::c/nonblank-string)
(s/def ::metric (c/only-keys :req-un [::name ::value]))
(s/def ::metrics (s/coll-of ::metric :min-count 1))

(s/def ::usage-event (c/only-keys :req-un [::c/id
                                           ::c/resourceURI
                                           ::c/acl

                                           ::cloud-vm-instanceid
                                           ::user
                                           ::cloud
                                           ::metrics]
                                  :opt-un [::c/created      ;; FIXME: should be required
                                           ::c/updated      ;; FIXME: should be required
                                           ::c/name
                                           ::c/description
                                           ::c/properties
                                           ::c/operations

                                           ::start-timestamp
                                           ::end-timestamp]))
