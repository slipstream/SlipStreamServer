(ns com.sixsq.slipstream.ssclj.resources.spec.description
  "Spec definitions for the descriptions of resources. These descriptions
   provide information to clients to allow them to understand the contents
   of resources."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;;
;; parameter descriptions for providing help information to clients
;; these definitions are the 'cimi.desc' namespace
;;

(s/def :cimi.desc/displayName :cimi.core/nonblank-string)
(s/def :cimi.desc/category :cimi.core/nonblank-string)
(s/def :cimi.desc/description :cimi.core/nonblank-string)
(s/def :cimi.desc/type #{"string" "boolean" "int" "float" "timestamp" "enum" "map" "list"})
(s/def :cimi.desc/mandatory boolean?)
(s/def :cimi.desc/readOnly boolean?)
(s/def :cimi.desc/order nat-int?)
(s/def :cimi.desc/enum (s/coll-of :cimi.core/nonblank-string :min-count 1))
(s/def :cimi.desc/instructions :cimi.core/nonblank-string)

(s/def :cimi.desc/parameter-description
  (su/only-keys :req-un [:cimi.desc/type]
                :opt-un [:cimi.desc/displayName
                         :cimi.desc/category
                         :cimi.desc/description
                         :cimi.desc/mandatory
                         :cimi.desc/readOnly
                         :cimi.desc/order
                         :cimi.desc/enum
                         :cimi.desc/instructions]))

(s/def :cimi.desc/resource-description
  (s/every (s/or :acl (s/tuple #{:acl} :cimi.common/acl)
                 :desc (s/tuple keyword? :cimi.desc/parameter-description))))

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
