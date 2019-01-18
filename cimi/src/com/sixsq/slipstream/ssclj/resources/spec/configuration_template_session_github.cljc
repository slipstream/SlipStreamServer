(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-github
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template :as ps]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def ::clientID
  (-> (st/spec ::cimi-core/token)
      (assoc :name "clientID"
             :json-schema/name "clientID"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "client ID"
             :json-schema/description "GitHub client ID"
             :json-schema/help "GitHub client ID associated with registered application"
             :json-schema/group "body"
             :json-schema/order 20
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::clientSecret
  (-> (st/spec ::cimi-core/token)
      (assoc :name "clientSecret"
             :json-schema/name "clientSecret"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "client secret"
             :json-schema/description "GitHub client secret"
             :json-schema/help "GitHub client secret associated with registered application"
             :json-schema/group "body"
             :json-schema/order 21
             :json-schema/hidden false
             :json-schema/sensitive true)))


(s/def ::instance
  (-> (st/spec ::ps/instance)
      (assoc :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable false
             :json-schema/consumerWritable true)))


(def configuration-template-keys-spec-req
  {:req-un [::instance ::clientID ::clientSecret]})

(def configuration-template-keys-spec-create
  {:req-un [::instance ::clientID ::clientSecret]})

;; Defines the contents of the github authentication ConfigurationTemplate resource itself.
(s/def ::session-github
  (su/only-keys-maps ps/resource-keys-spec
                     configuration-template-keys-spec-req))

;; Defines the contents of the github authentication template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :configurationTemplate here.
(s/def ::configurationTemplate
  (su/only-keys-maps ps/template-keys-spec
                     configuration-template-keys-spec-create))

(s/def ::session-github-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::configurationTemplate]}))
