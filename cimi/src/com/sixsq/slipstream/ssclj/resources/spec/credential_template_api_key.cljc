(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-api-key
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def :cimi.credential-template.api-key/ttl
  (-> (st/spec nat-int?)
      (assoc :name "ttl"
             :json-schema/name "ttl"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "integer"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "TTL"
             :json-schema/description "Time to Live (TTL) for API key/secret"
             :json-schema/help "Time to Live (TTL) for created API key/secret"
             :json-schema/group "body"
             :json-schema/order 20
             :json-schema/hidden false
             :json-schema/sensitive false)))


(def credential-template-keys-spec
  {:opt-un [:cimi.credential-template.api-key/ttl]})

(def credential-template-create-keys-spec
  {:opt-un [:cimi.credential-template.api-key/ttl]})

;; Defines the contents of the api-key CredentialTemplate resource itself.
(s/def :cimi/credential-template.api-key
  (su/only-keys-maps ps/resource-keys-spec
                     credential-template-keys-spec))

;; Defines the contents of the api-key template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :credentialTemplate here.
(s/def :cimi.credential-template.api-key/credentialTemplate
  (su/only-keys-maps ps/template-keys-spec
                     credential-template-create-keys-spec))

(s/def :cimi/credential-template.api-key-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.credential-template.api-key/credentialTemplate]}))
