(ns com.sixsq.slipstream.ssclj.resources.spec.callback
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def ::action
  (-> (st/spec ::cimi-core/nonblank-string)
      (assoc :name "action"
             :json-schema/name "action"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable false
             :json-schema/consumerWritable true

             :json-schema/displayName "action"
             :json-schema/description "name of action"
             :json-schema/help "name of action performed by callback"
             :json-schema/group "body"
             :json-schema/order 10
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::state
  (-> (st/spec #{"WAITING" "FAILED" "SUCCEEDED"})
      (assoc :name "state"
             :json-schema/name "state"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "state"
             :json-schema/description "current state of callback"
             :json-schema/help "current state of callback"
             :json-schema/group "body"
             :json-schema/order 11
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::targetResource
  (-> (st/spec ::cimi-common/resource-link)
      (assoc :name "targetResource"
             :json-schema/name "targetResource"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "map"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable false
             :json-schema/consumerWritable true

             :json-schema/displayName "target resource"
             :json-schema/description "reference to resource affected by callback"
             :json-schema/help "reference to resource affected by callback"
             :json-schema/group "body"
             :json-schema/order 12
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::data
  (-> (st/spec (su/constrained-map keyword? any?))
      (assoc :name "data"
             :json-schema/name "data"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "map"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable false
             :json-schema/consumerWritable true
             :json-schema/indexed false

             :json-schema/displayName "data"
             :json-schema/description "data required for callback"
             :json-schema/help "data required to execute the callback action"
             :json-schema/group "body"
             :json-schema/order 13
             :json-schema/hidden false
             :json-schema/sensitive false)))




(s/def ::expires
  (-> (st/spec ::cimi-core/timestamp)
      (assoc :name "expires"
             :json-schema/name "expires"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable false
             :json-schema/consumerWritable true

             :json-schema/displayName "expires"
             :json-schema/description "expiry timestamp for callback action"
             :json-schema/help "expiry timestamp for callback action"
             :json-schema/group "body"
             :json-schema/order 14
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::callback
  (su/only-keys-maps cimi-common/common-attrs
                     {:req-un [::action
                               ::state]
                      :opt-un [::targetResource
                               ::data
                               ::expires]}))
