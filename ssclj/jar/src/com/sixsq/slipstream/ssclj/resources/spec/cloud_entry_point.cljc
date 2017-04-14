(ns com.sixsq.slipstream.ssclj.resources.spec.cloud-entry-point
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.cep/baseURI :cimi.core/nonblank-string)

;; FIXME: Duplication is painful, is there a better way?
(s/def :cimi.cep/cloud-entry-point
  (s/merge
    (s/every (s/or :common-attr (s/tuple #{:id
                                           :resourceURI
                                           :created
                                           :updated
                                           :name
                                           :description
                                           :properties
                                           :operations
                                           :acl
                                           :baseURI}
                                         any?)
                   :link (s/tuple keyword? :cimi.common/resource-link)))
    (s/keys :req-un [:cimi.common/id
                     :cimi.common/resourceURI
                     :cimi.common/created
                     :cimi.common/updated
                     :cimi.acl/acl

                     :cimi.cep/baseURI]
            :opt-un [:cimi.common/name
                     :cimi.common/description
                     :cimi.common/properties
                     :cimi.common/operations])))
