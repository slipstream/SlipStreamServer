(ns com.sixsq.slipstream.ssclj.resources.spec.user-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.ui-hints :as hints]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


;; All user templates must indicate the method used to create the user.
(s/def ::method
  (-> (st/spec ::cimi-core/identifier)
      (assoc :name "method"
             :type :string
             :json-schema/name "method"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "method"
             :json-schema/description "user creation method"
             :json-schema/help "user creation method"
             :json-schema/group "body"
             :json-schema/order 0
             :json-schema/hidden true
             :json-schema/sensitive false)))


;; All user template resources must have a 'instance' attribute that is used as
;; the template identifier.
(s/def ::instance
  (-> (st/spec ::cimi-core/identifier)
      (assoc :name "instance"
             :type :string
             :json-schema/name "instance"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "instance"
             :json-schema/description "instance name of user creation method"
             :json-schema/help "instance name of user creation method"
             :json-schema/group "body"
             :json-schema/order 1
             :json-schema/hidden true
             :json-schema/sensitive false)))


(def user-template-regex #"^user-template/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$")

(s/def ::href
  (-> (st/spec (s/and string? #(re-matches user-template-regex %)))
      (assoc :name "href"
             :type :map
             :json-schema/name "href"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "map"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "user template"
             :json-schema/description "reference to the user template"
             :json-schema/help "reference to the user template"
             :json-schema/group "body"
             :json-schema/order 0
             :json-schema/hidden true
             :json-schema/sensitive false)))

;;
;; Keys specifications for UserTemplate resources.
;; As this is a "base class" for UserTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def user-template-keys-spec {:req-un [::method ::instance]})

(def user-template-template-keys-spec {:req-un [::instance]
                                       :opt-un [::method]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        hints/ui-hints-spec
                        user-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

;; subclasses MUST provide the href to the template to use
(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        hints/ui-hints-spec
                        user-template-template-keys-spec]))

