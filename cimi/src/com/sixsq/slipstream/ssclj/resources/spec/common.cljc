(ns com.sixsq.slipstream.ssclj.resources.spec.common
  "Spec definitions for common types used in CIMI resources."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.acl :as cimi-acl]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.resources.spec.common-operation :as cimi-common-operation]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def ::id
  (-> (st/spec ::cimi-core/resource-href)
      (assoc :name "id"
             :type :string
             :json-schema/name "id"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory false
             :json-schema/mutable false
             :json-schema/consumerWritable false

             :json-schema/displayName "identifier"
             :json-schema/description "unique resource identifier"
             :json-schema/help "unique resource identifier generated by server"
             :json-schema/group "metadata"
             :json-schema/category "CIMI common attributes"
             :json-schema/order 0
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::resourceURI
  (-> (st/spec ::cimi-core/uri)
      (assoc :name "resourceURI"
             :type :uri
             :json-schema/name "resourceURI"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "URI"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory false
             :json-schema/mutable false
             :json-schema/consumerWritable false

             :json-schema/displayName "resource URI"
             :json-schema/description "URI for resource type"
             :json-schema/help "unique resource identifier uniquely identifying the resource type"
             :json-schema/group "metadata"
             :json-schema/category "CIMI common attributes"
             :json-schema/order 1
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::created
  (-> (st/spec ::cimi-core/timestamp)
      (assoc :name "created"
             :type :string
             :json-schema/name "created"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "dateTime"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory false
             :json-schema/mutable false
             :json-schema/consumerWritable false

             :json-schema/displayName "created"
             :json-schema/description "creation timestamp (UTC) for resource"
             :json-schema/help "creation timestamp (UTC) for resource, managed by the server"
             :json-schema/group "metadata"
             :json-schema/category "CIMI common attributes"
             :json-schema/order 2
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::updated
  (-> (st/spec ::cimi-core/timestamp)
      (assoc :name "updated"
             :type :string
             :json-schema/name "updated"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "dateTime"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable false

             :json-schema/displayName "updated"
             :json-schema/description "latest resource update timestamp (UTC)"
             :json-schema/help "timestamp (UTC) most recent resource update, managed by the server"
             :json-schema/group "metadata"
             :json-schema/category "CIMI common attributes"
             :json-schema/order 3
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::name
  (-> (st/spec ::cimi-core/nonblank-string)
      (assoc :name "name"
             :type :string
             :json-schema/name "name"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "string"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "name"
             :json-schema/description "short, human-readable name for resource"
             :json-schema/help "optional short, human-readable name for resource"
             :json-schema/group "metadata"
             :json-schema/category "CIMI common attributes"
             :json-schema/order 4
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::description
  (-> (st/spec ::cimi-core/text)
      (assoc :name "description"
             :type :string
             :json-schema/name "description"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "string"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "description"
             :json-schema/description "human-readable description of resource"
             :json-schema/help "optional human-readable description for resource"
             :json-schema/group "metadata"
             :json-schema/category "CIMI common attributes"
             :json-schema/order 5
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::href
  (-> (st/spec ::cimi-core/resource-href)
      (assoc :name "href"
             :type :uri
             :json-schema/name "href"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "URI"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "href"
             :json-schema/description "reference to another resource"
             :json-schema/help "reference to the unique resource identifier of another resource"
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::resource-link
  (-> (st/spec (s/keys :req-un [::href]))
      (assoc :name "resourceLink"
             :type :map
             :json-schema/name "resourceLink"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "map"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "resourceLink"
             :json-schema/description "map containing a reference (href) to a resource"
             :json-schema/help "map containing a reference (href) to the unique resource identifier of another resource"
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::resource-links
  (-> (st/spec (s/coll-of ::resource-link :min-count 1))
      (assoc :name "resourceLinks"
             :type :vector
             :json-schema/name "resourceLinks"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "Array"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "resourceLinks"
             :json-schema/description "list of resourceLinks"
             :json-schema/help "list of resourceLinks"
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::operation
  (-> (st/spec (su/only-keys :req-un [::cimi-common-operation/href
                                      ::cimi-common-operation/rel]))
      (assoc :name "operation"
             :type :map
             :json-schema/name "operation"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "map"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable false

             :json-schema/displayName "operation"
             :json-schema/description "operation definition (name, URL) for a resource"
             :json-schema/help "definition (name, URL) of an available operation for a resource"
             :json-schema/group "operations"
             :json-schema/category "CIMI operations"
             :json-schema/order 0
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::operations
  (-> (st/spec (s/coll-of ::operation :min-count 1))
      (assoc :name "operations"
             :type :vector
             :json-schema/name "operations"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "Array"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable false

             :json-schema/displayName "operations"
             :json-schema/description "list of resource operations"
             :json-schema/help "list of available resource operations"
             :json-schema/group "operations"
             :json-schema/category "CIMI operations"
             :json-schema/order 0
             :json-schema/hidden false
             :json-schema/sensitive false

             :slipstream.es/mapping {:type "object", :enabled false})))


(s/def ::kw-or-str (s/or :keyword keyword? :string ::cimi-core/nonblank-string))


(s/def ::properties
  (-> (st/spec (s/map-of ::kw-or-str string? :min-count 1))
      (assoc :name "properties"
             :type :map
             :json-schema/name "properties"
             :json-schema/namespace common-ns/cimi-namespace
             :json-schema/uri common-ns/cimi-uri
             :json-schema/type "map"
             :json-schema/providerMandatory false
             :json-schema/consumerMandatory false
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "properties"
             :json-schema/description "client defined properties of the resource"
             :json-schema/help "client defined properties (key, value) of the resource"
             :json-schema/group "metadata"
             :json-schema/category "CIMI common attributes"
             :json-schema/order 15
             :json-schema/hidden false
             :json-schema/sensitive false)))


(s/def ::acl (su/only-keys :req-un [::cimi-acl/owner]
                           :opt-un [::cimi-acl/rules]))


#_(s/def ::acl
    (-> (st/spec (su/only-keys :req-un [::cimi-acl/owner]
                               :opt-un [::cimi-acl/rules]))
        (assoc :name "acl"
               :type :map
               :json-schema/name "acl"
               :json-schema/namespace common-ns/cimi-namespace
               :json-schema/uri common-ns/cimi-uri
               :json-schema/type "map"
               :json-schema/providerMandatory true
               :json-schema/consumerMandatory false
               :json-schema/mutable true
               :json-schema/consumerWritable true

               :json-schema/displayName "ACL"
               :json-schema/description "Access Control List for the resource"
               :json-schema/help "Access Control List (ACL) for the resource"
               :json-schema/group "acl"
               :json-schema/category "Access Control List"
               :json-schema/order 0
               :json-schema/hidden false
               :json-schema/sensitive false)))


(def ^:const common-attrs
  "clojure.spec/keys specification (as a map) for common CIMI attributes
   for regular resources"
  {:req-un [::id
            ::resourceURI
            ::created
            ::updated
            ::acl]
   :opt-un [::name
            ::description
            ::properties
            ::operations]})


(def ^:const create-attrs
  "clojure.spec/keys specification (as a map) for common CIMI attributes
   for the 'create' resources used when creating resources from a template.
   This applies to the create wrapper and not the embedded resource
   template!"
  {:req-un [::resourceURI]
   :opt-un [::name
            ::description
            ::created
            ::updated
            ::properties
            ::operations
            ::acl]})


(def ^:const template-attrs
  "The clojure.spec/keys specification (as a map) for common CIMI attributes
   for the resource templates that are embedded in 'create' resources. Although
   these may be added to the templates (usually by reference), they will have
   no affect on the created resource."
  {:opt-un [::resourceURI
            ::name
            ::description
            ::created
            ::updated
            ::properties
            ::operations
            ::acl]})
