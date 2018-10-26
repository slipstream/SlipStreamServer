(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-api-key
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def ::key
  (-> (st/spec ::cimi-core/nonblank-string)
      (assoc :name "key"
             :type :string
             :json-schema/name "key"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "key"
             :json-schema/description "key for API key/secret pair"
             :json-schema/help "key for API key/secret pair"
             :json-schema/group "body"
             :json-schema/order 20
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::secret
  (-> (st/spec ::cimi-core/nonblank-string)
      (assoc :name "secret"
             :type :string
             :json-schema/name "secret"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "secret"
             :json-schema/description "secret for API key/secret pair"
             :json-schema/help "secret for API key/secret pair"
             :json-schema/group "body"
             :json-schema/order 21
             :json-schema/hidden false
             :json-schema/sensitive true)))


;; all parameters must be specified in both the template and the create resource
(def session-template-keys-spec
  {:req-un [::key ::secret]})

;; Defines the contents of the api-key SessionTemplate resource itself.
(s/def ::api-key
  (su/only-keys-maps ps/resource-keys-spec
                     session-template-keys-spec))

;; Defines the contents of the api-key template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def ::sessionTemplate
  (su/only-keys-maps ps/template-keys-spec
                     session-template-keys-spec))

(s/def ::api-key-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::sessionTemplate]}))
