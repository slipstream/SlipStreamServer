(ns com.sixsq.slipstream.ssclj.resources.usage.spec
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.common.spec :as c]))

(s/def ::user ::c/nonblank-string)
(s/def ::cloud ::c/nonblank-string)
(s/def ::start-timestamp ::c/timestamp)
(s/def ::end-timestamp ::c/timestamp)
(s/def ::usage ::c/nonblank-string)
(s/def ::grouping ::c/nonblank-string)
(s/def ::frequency ::c/nonblank-string)

(s/def ::usage (c/only-keys :req-un [::c/id
                                     ::c/resourceURI
                                     ::c/created
                                     ::c/updated
                                     ::c/acl

                                     ::user
                                     ::cloud
                                     ::start-timestamp
                                     ::end-timestamp
                                     ::usage
                                     ::grouping
                                     ::frequency]
                            :opt-un [::c/name
                                     ::c/description
                                     ::c/properties
                                     ::c/operations]))
