(ns com.sixsq.slipstream.ssclj.resources.common.schema
  (:require
    [superstring.core                                     :as str]
    [schema.core                                        :as s]
    [com.sixsq.slipstream.ssclj.resources.common.utils  :as u]))

(def ^:const slipstream-schema-uri "http://sixsq.com/slipstream/1/")

;; using draft 2.0.0c
(def ^:const cimi-schema-uri "http://schemas.dmtf.org/cimi/2/")

;;
;; actions
;;

(def ^:const valid-actions
  #{:add :edit :delete
    :start :stop :restart :pause :suspend
    :export :import :capture :snapshot})

(def ^:const action-uri
  (let [root "http://sixsq.com/slipstream/1/Action/"]
    (into {} (map (fn [k] [k (str root (name k))]) valid-actions))))

(def ^:const valid-action-uris
  (vals action-uri))

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
