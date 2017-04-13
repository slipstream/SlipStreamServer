(ns com.sixsq.slipstream.ssclj.resources.common.spec
  (:require
    [clojure.set :as set]
    [clojure.spec :as s]
    [clojure.spec.gen :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]))

;;
;; merges keys specifications
;;
(defn merge-keys-specs
  "Takes a number of s/keys specifications given as maps, merges
   the specifications by creating the union of the sets and then
   produces a keyword sequence of the combined result."
  [& specs]
  (apply concat (apply merge-with set/union specs)))

;;
;; creates a closed map definition: only defined keys are permitted
;; (implementation provided on clojure mailing list by Alistair Roche)
;;
(defmacro only-keys
  [& {:keys [req req-un opt opt-un] :as args}]
  `(s/merge (s/keys ~@(apply concat (vec args)))
            (s/map-of ~(set (concat req
                                    (map (comp keyword name) req-un)
                                    opt
                                    (map (comp keyword name) opt-un)))
                      any?)))

#_(defn only-keys-maps
  [& map-specs]
  (let [{:keys [req req-un opt opt-un] :as map-spec} (apply merge-with set/union map-specs)
        args (apply concat map-spec)]
    `(s/merge (s/keys ~@(apply concat (vec args)))
              (s/map-of ~(set (concat req
                                      (map (comp keyword name) req-un)
                                      opt
                                      (map (comp keyword name) opt-un)))
                        any?))))


;;
;; schema definitions for basic types
;;

(s/def :cimi.core/nonblank-string (s/and string? (complement str/blank?)))

(s/def :cimi.core/port (s/int-in 1 65536))

;; FIXME: Provide an implementation that works with ClojureScript.
(s/def :cimi.core/timestamp (s/with-gen (s/and string? cu/parse-timestamp)
                                        (constantly (gen/fmap cu/unparse-timestamp (gen/gen-for-pred inst?)))))

;; FIXME: This should not be needed.
(s/def :cimi.core/optional-timestamp (s/or :empty #{""} :not-empty ::timestamp))

;; FIXME: This is a marker spec that should eventually be turned into a real one.
(s/def :cimi.core/uri :cimi.core/nonblank-string)

;;
;; A resource type is a kebab-cased name consisting of only lowercase letters.
;;
(def resource-type-regex #"^[a-z]([a-z-]*[a-z])?$")
(def char-resource-type (gen/fmap char (s/gen (set (concat (range 97 123) [45])))))
(def gen-resource-type (gen/fmap str/join (gen/vector char-resource-type)))
(s/def :cimi.core/resource-type (s/with-gen (s/and string? #(re-matches resource-type-regex %))
                                            (constantly gen-resource-type)))

;;
;; A resource identifier consists of one or more letters, digits, underscores, or dashes.
;; The identifier cannot start or end with an underscore or dash.
;;
(def resource-identifier-regex #"^[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$")
(def char-resource-identifier (gen/fmap char (s/gen (set (concat (range 48 58) (range 65 91) (range 97 123) [45 95])))))
(def gen-resource-identifier (gen/fmap str/join (gen/vector char-resource-identifier)))
(s/def :cimi.core/resource-identifier (s/with-gen (s/and string? #(re-matches resource-identifier-regex %))
                                                  (constantly gen-resource-identifier)))

;;
;; A resource href is the concatenation of a resource type and resource identifier separated
;; with a slash.  The later part is optional for singleton resources like the cloud-entry-point.
;;
(defn join-href-parts
  [[resource-type resource-identifier]]
  (if resource-identifier
    (str resource-type "/" resource-identifier)
    resource-type))

(def resource-href-regex #"^[a-z]([a-z-]*[a-z])?(/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?)?$")
(def gen-resource-href (gen/fmap join-href-parts (gen/tuple
                                                   gen-resource-type
                                                   (gen/frequency [[95 gen-resource-identifier]
                                                                   [5 (s/gen #{nil})]]))))
(s/def :cimi.core/resource-href (s/with-gen (s/and string? #(re-matches resource-href-regex %))
                                            (constantly gen-resource-href)))

;;
;; schema definitions for common attributes
;;

(s/def :cimi.common.link/href :cimi.core/resource-href)

(s/def :cimi.common/resource-link (s/keys :req-un [:cimi.common.link/href]))
(s/def :cimi.common/resource-links (s/coll-of :cimi.common/resource-link :min-count 1))

(s/def :cimi.common.operation/href :cimi.core/nonblank-string)
(s/def :cimi.common.operation/rel :cimi.core/nonblank-string)
(s/def :cimi.common/operation (only-keys :req-un [:cimi.common.operation/href
                                                  :cimi.common.operation/rel]))
(s/def :cimi.common/operations (s/coll-of :cimi.common/operation :min-count 1))

(s/def :cimi.common/kw-or-str (s/or :keyword keyword? :string :cimi.core/nonblank-string))
(s/def :cimi.common/properties (s/map-of :cimi.common/kw-or-str string? :min-count 1))

;;
;; Common Attributes
;;

;;
;; These attributes are common to all resources except the
;; CloudEntryPoint.  When these attributes are passed into the
;; CIMI service implementation, the required entries and the
;; :operations will be replaced by the service-generated values.
;;

(s/def :cimi.common/id :cimi.core/resource-href)
(s/def :cimi.common/resourceURI :cimi.core/uri)
(s/def :cimi.common/created :cimi.core/timestamp)
(s/def :cimi.common/updated :cimi.core/timestamp)
(s/def :cimi.common/name :cimi.core/nonblank-string)
(s/def :cimi.common/description :cimi.core/nonblank-string)

(def common-attrs {:req-un [:cimi.common/id
                            :cimi.common/resourceURI
                            :cimi.common/created
                            :cimi.common/updated]
                   :opt-un [:cimi.common/name
                            :cimi.common/description
                            :cimi.common/properties
                            :cimi.common/operations]})

;;
;; These are the common attributes for create resources.
;; All of the common attributes are allowed, but the optional
;; ones other than :name and :description will be ignored.
;;
(def create-attrs {:req-un [:cimi.common/resourceURI]
                   :opt-un [:cimi.common/name
                            :cimi.common/description
                            :cimi.common/created
                            :cimi.common/updated
                            :cimi.common/properties
                            :cimi.common/operations]})

;;
;; Ownership and access control
;;
;; These are additions to the standard CIMI schema for the
;; SlipStream implementation.
;;

(def principal-regex #"^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?$")
(def char-principal (gen/fmap char (s/gen (set (concat (range 65 91) (range 97 123) (range 48 58) [45 46 95])))))
(def gen-principal (gen/fmap str/join (gen/vector char-principal 1 255)))
(s/def :cimi.acl/principal (s/with-gen (s/and string? #(re-matches principal-regex %))
                                       (constantly gen-principal)))

(s/def :cimi.acl/type #{"USER" "ROLE"})
(s/def :cimi.acl/right #{"ALL" "VIEW" "MODIFY"})


(s/def :cimi.acl/owner (only-keys :req-un [:cimi.acl/principal :cimi.acl/type]))

(s/def :cimi.acl/rule (only-keys :req-un [:cimi.acl/principal
                                          :cimi.acl/type
                                          :cimi.acl/right]))
(s/def :cimi.acl/rules (s/coll-of :cimi.acl/rule :min-count 1))

(s/def :cimi.acl/acl (only-keys :req-un [:cimi.acl/owner]
                                :opt-un [:cimi.acl/rules]))


;;
;; parameter descriptions
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
  (only-keys :req-un [:cimi.desc/type]
             :opt-un [:cimi.desc/displayName
                      :cimi.desc/category
                      :cimi.desc/description
                      :cimi.desc/mandatory
                      :cimi.desc/readOnly
                      :cimi.desc/order
                      :cimi.desc/enum
                      :cimi.desc/instructions]))

(s/def :cimi.desc/resource-description
  (s/every (s/or :acl (s/tuple #{:acl} :cimi.acl/acl)
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

