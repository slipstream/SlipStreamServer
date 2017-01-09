(ns com.sixsq.slipstream.ssclj.resources.cloud-entry-point.spec
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.common.spec :as c]))

(s/def ::baseURI ::c/nonblank-string)

;; FIXME: Duplication is painful, is there a better way?
(s/def ::cloud-entry-point (s/merge
                             (s/every (s/or :common-attr (s/tuple #{:id
                                                                    :resourceURI
                                                                    :created
                                                                    :updated
                                                                    :acl
                                                                    :name
                                                                    :description
                                                                    :properties
                                                                    :operations
                                                                    :baseURI}
                                                                  any?)
                                            :link (s/tuple keyword? ::c/resource-link)))
                             (s/keys :req-un [::c/id
                                              ::c/resourceURI
                                              ::c/created
                                              ::c/updated
                                              ::c/acl

                                              ::baseURI]
                                     :opt-un [::c/name
                                              ::c/description
                                              ::c/properties
                                              ::c/operations])))
