(ns com.sixsq.slipstream.ssclj.resources.spec.email
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def ::address
  (-> (st/spec ::cimi-core/email)
      (assoc :name "address"
             :type :string
             :json-schema/name "address"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "address"
             :json-schema/description "email address"
             :json-schema/help "unique email address"
             :json-schema/group "body"
             :json-schema/order 10
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::validated
  (-> (st/spec boolean?)
      (assoc :name "validated"
             :type :string
             :json-schema/name "validated"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable false

             :json-schema/displayName "validated"
             :json-schema/description "validated email address?"
             :json-schema/help "flag indicating if the associated email address has been validated"
             :json-schema/group "body"
             :json-schema/order 11
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::schema
  (su/only-keys-maps c/common-attrs
                     {:req-un [::address]
                      :opt-un [::validated]}))
