(ns com.sixsq.slipstream.ssclj.resources.usage-record.spec
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.common.spec :as c]))

(s/def ::cloud-vm-instanceid ::c/nonblank-string)
(s/def ::user ::c/nonblank-string)
(s/def ::cloud ::c/nonblank-string)
(s/def ::metric-name ::c/nonblank-string)
(s/def ::metric-value ::c/nonblank-string)

(s/def ::start-timestamp ::c/timestamp)
(s/def ::end-timestamp ::c/optional-timestamp)

(s/def ::usage-record (c/only-keys :req-un [::c/id
                                            ::c/resourceURI
                                            ::c/acl

                                            ::cloud-vm-instanceid
                                            ::user
                                            ::cloud
                                            ::metric-name
                                            ::metric-value]
                                   :opt-un [::c/name
                                            ::c/description
                                            ::c/created
                                            ::c/updated
                                            ::c/properties
                                            ::c/operations

                                            ::start-timestamp
                                            ::end-timestamp]))
