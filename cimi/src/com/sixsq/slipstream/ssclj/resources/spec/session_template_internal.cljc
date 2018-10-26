(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-internal
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def ::username
  (-> (st/spec ::cimi-core/nonblank-string)
      (assoc :name "username"
             :type :string
             :json-schema/name "username"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "username"
             :json-schema/description "username for your account"
             :json-schema/help "username for your account"
             :json-schema/group "body"
             :json-schema/order 20
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::password
  (-> (st/spec ::cimi-core/nonblank-string)
      (assoc :name "password"
             :type :string
             :json-schema/name "password"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "password"
             :json-schema/description "password for your account"
             :json-schema/help "password for your account"
             :json-schema/group "body"
             :json-schema/order 21
             :json-schema/hidden false
             :json-schema/sensitive true)))


;; all parameters must be specified in both the template and the create resource
(def session-template-keys-spec-req
  {:req-un [::username ::password]})

;; Defines the contents of the internal SessionTemplate resource itself.
(s/def ::internal
  (su/only-keys-maps ps/resource-keys-spec
                     session-template-keys-spec-req))

;; Defines the contents of the internal template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def ::sessionTemplate
  (su/only-keys-maps ps/template-keys-spec
                     session-template-keys-spec-req))

(s/def ::internal-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::sessionTemplate]}))
