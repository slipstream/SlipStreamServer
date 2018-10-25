(ns com.sixsq.slipstream.ssclj.resources.spec.acl
  "Access Control Lists (an extension to the CIMI standard)."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def ::principal ::cimi-core/nonblank-string)


(s/def ::type
  (st/spec #{"USER" "ROLE"}))


(s/def ::right
  (st/spec #{"VIEW" "MODIFY"                                ;; LEGACY RIGHTS
             "ALL"
             "VIEW_ACL" "VIEW_DATA" "VIEW_META"
             "EDIT_ACL" "EDIT_DATA" "EDIT_META"
             "DELETE"
             "MANAGE"}))


(s/def ::owner
  (-> (st/spec (su/only-keys :req-un [::principal ::type]))
      (assoc :name "owner"
             :type :map
             :json-schema/name "owner"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "map"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "owner"
             :json-schema/description "owner identifier for resource"
             :json-schema/help "owner identifier for resource"
             :json-schema/group "acl"
             :json-schema/category "Access Control List"
             :json-schema/order 10
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::rule
  (-> (st/spec (su/only-keys :req-un [::principal
                                      ::type
                                      ::right]))
      (assoc :name "rule"
             :type :map
             :json-schema/name "rule"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "map"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "rule"
             :json-schema/description "rule for Access Control List"
             :json-schema/help "rule for Access Control List"
             :json-schema/group "acl"
             :json-schema/category "Access Control List"
             :json-schema/order 11
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::rules
  (-> (st/spec (s/coll-of ::rule :min-count 1 :kind vector?))
      (assoc :name "rules"
             :type :vector
             :json-schema/name "rules"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "Array"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "rules"
             :json-schema/description "rules for Access Control List"
             :json-schema/help "rules for Access Control List"
             :json-schema/group "acl"
             :json-schema/category "Access Control List"
             :json-schema/order 12
             :json-schema/hidden false
             :json-schema/sensitive false)))



