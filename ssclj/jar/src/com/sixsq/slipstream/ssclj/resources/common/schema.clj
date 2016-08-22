(ns com.sixsq.slipstream.ssclj.resources.common.schema
  (:require
    [superstring.core :as str]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [clojure.set :as set]))

(def ^:const slipstream-schema-uri "http://sixsq.com/slipstream/1/")

;; using draft 2.0.0c
(def ^:const cimi-schema-uri "http://schemas.dmtf.org/cimi/2/")

;;
;; actions
;;

;; core actions do not have a URI prefix
(def ^:const core-actions
  #{:add :edit :delete :insert :remove})

;; additional resource actions have a URI prefix
(def ^:const action-prefix (str cimi-schema-uri "action/"))
(def ^:const prefixed-actions
  #{:start :stop :restart :pause :suspend
    :export :import :capture :snapshot
    :forceSync :swapBackup :restore :enable :disable})

;; implementation-specific resource actions have a prefix
(def ^:const impl-action-prefix (str slipstream-schema-uri "action/"))
(def ^:const impl-prefixed-actions
  #{:describe})

(def ^:const action-uri
  (doall
    (merge
      (into {} (map (juxt identity name) core-actions))
      (into {} (map (juxt identity #(str action-prefix (name %))) prefixed-actions))
      (into {} (map (juxt identity #(str impl-action-prefix (name %))) impl-prefixed-actions)))))

;;
;; schema definitions for basic types
;;

(def PosInt
  (s/constrained s/Int pos?))

(def NonNegInt
  (s/constrained s/Int (complement neg?)))

(def NonBlankString
  (s/constrained s/Str (complement str/blank?)))

(def BlankString
  (s/constrained s/Str str/blank?))

(def NonEmptyStrList
  (s/constrained [NonBlankString] seq 'not-empty?))

(def Timestamp
  (s/constrained NonBlankString u/valid-timestamp? 'valid-timestamp?))

(def OptionalTimestamp
  (s/constrained s/Str (fn [x] (or (str/blank? x) (u/valid-timestamp? x))) 'valid-timestamp-or-blank?))

(def KeywordOrString
  (s/pred (fn [x] (or (keyword? x) (string? x))) 'keyword-or-string?))

;;
;; schema definitions for common attributes
;;

(def ResourceLink
  {:href NonBlankString})

(def ResourceLinks
  (s/constrained [ResourceLink] seq 'not-empty?))

(def Operation
  (merge ResourceLink {:rel NonBlankString}))

(def Operations
  (s/constrained [Operation] seq 'not-empty?))

(def Properties
  (s/constrained
    {KeywordOrString s/Str}
    seq 'not-empty?))

;;
;; Common Attributes
;;

;;
;; These attributes are common to all resources except the
;; CloudEntryPoint.  When these attributes are passed into the
;; CIMI service implementation, the required entries and the
;; :operations will be replaced by the service-generated values.
;;
(def CommonAttrs
  {:id                           NonBlankString
   :resourceURI                  NonBlankString
   (s/optional-key :name)        NonBlankString
   (s/optional-key :description) NonBlankString
   :created                      Timestamp
   :updated                      Timestamp
   (s/optional-key :properties)  Properties
   (s/optional-key :operations)  Operations})

;;
;; These are the common attributes for create resources.
;; All of the common attributes are allowed, but the optional
;; ones other than :name and :description will be ignored.
;;
(def CreateAttrs
  {:resourceURI                  NonBlankString
   (s/optional-key :name)        NonBlankString
   (s/optional-key :description) NonBlankString
   (s/optional-key :created)     Timestamp
   (s/optional-key :updated)     Timestamp
   (s/optional-key :properties)  Properties
   (s/optional-key :operations)  Operations})

;;
;; Ownership and access control
;;
;; These are additions to the standard CIMI schema for the
;; StratusLab implementation.
;;

(def access-control-types (s/enum "USER" "ROLE"))

(def access-control-rights (s/enum "ALL" "VIEW" "MODIFY"))

(def AccessControlId
  {:principal NonBlankString
   :type      access-control-types})

(def AccessControlRule
  (merge AccessControlId {:right access-control-rights}))

(def AccessControlRules
  (s/constrained [AccessControlRule] seq 'not-empty?))

(def AccessControlList
  {:owner                  AccessControlId
   (s/optional-key :rules) AccessControlRules})

(def AclAttr
  {:acl AccessControlList})

;;
;; parameter descriptions
;;

(def ParameterTypes (s/enum "string" "boolean" "int" "float" "timestamp" "enum" "map" "list"))

(def ParameterDescription
  {:displayName                  NonBlankString
   (s/optional-key :category)    NonBlankString
   (s/optional-key :description) NonBlankString
   :type                         ParameterTypes
   (s/optional-key :mandatory)   s/Bool
   (s/optional-key :readOnly)    s/Bool
   (s/optional-key :order)       NonNegInt
   (s/optional-key :enum)        [NonBlankString]})

(def ResourceDescription
  (merge AclAttr
         {s/Keyword ParameterDescription}))

(def CommonParameterDescription
  {:id          {:displayName "ID"
                 :category    "common"
                 :description "unique resource identifier"
                 :type        "string"
                 :mandatory   true
                 :readOnly    true
                 :order       0}
   :resourceURI {:displayName "Resource URI"
                 :category    "common"
                 :description "type identifier as a URI"
                 :type        "string"
                 :mandatory   true
                 :readOnly    true
                 :order       1}
   :name        {:displayName "Name"
                 :category    "common"
                 :description "human-readable name"
                 :type        "string"
                 :mandatory   false
                 :readOnly    false
                 :order       2}
   :description {:displayName "Description"
                 :category    "common"
                 :description "short, human-readable description"
                 :type        "string"
                 :mandatory   false
                 :readOnly    false
                 :order       3}
   :created     {:displayName "Created"
                 :category    "common"
                 :description "creation timestamp"
                 :type        "timestamp"
                 :mandatory   true
                 :readOnly    true
                 :order       4}
   :updated     {:displayName "Updated"
                 :category    "common"
                 :description "update timestamp"
                 :type        "timestamp"
                 :mandatory   true
                 :readOnly    true
                 :order       5}
   :properties  {:displayName "Properties"
                 :category    "common"
                 :description "user-defined properties"
                 :type        "map"
                 :mandatory   false
                 :readOnly    false
                 :order       6}
   :operations  {:displayName "Operation"
                 :category    "common"
                 :description "allowed actions"
                 :type        "list"
                 :mandatory   true
                 :readOnly    true
                 :order       7}})

