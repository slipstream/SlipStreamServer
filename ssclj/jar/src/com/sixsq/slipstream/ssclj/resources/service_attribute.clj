(ns com.sixsq.slipstream.ssclj.resources.service-attribute
  (:require
    [superstring.core :as str]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :as san]
    [ring.util.response :as r])
  (:import
    [java.math BigInteger]
    [java.net URI URISyntaxException]
    [java.nio.charset Charset]))

(def ^:const resource-name "ServiceAttribute")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ServiceAttributeCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})
;;
;; schemas
;;

(def LocalizedEntry
  {:name                        c/NonBlankString
   :description                 c/NonBlankString
   (s/optional-key :categories) c/NonEmptyStrList})

(def LocalizedEntries
  (s/constrained {s/Keyword LocalizedEntry} seq 'not-empty?))

(def CompositeType
  (s/constrained [c/NonBlankString] seq 'not-empty?))

;; TODO: Determine how to force schema to have at least one localized entry
(def Attribute
  (merge c/CommonAttrs
         c/AclAttr
         {:prefix                     c/NonBlankString
          :attr-name                  c/NonBlankString

          :type                       (s/cond-pre s/Str CompositeType)
          (s/optional-key :authority) c/NonBlankString
          :major-version              c/NonNegInt
          :minor-version              c/NonNegInt
          :patch-version              c/NonNegInt
          :normative                  s/Bool
          s/Keyword                   LocalizedEntry}))

;;
;; multimethods for validation and operations
;;

(defn validate-attribute-namespace
  [resource]
  (if ((san/all-prefixes) (:prefix resource))
    resource
    (let [code 406
          msg (str "resource attributes do not satisfy defined namespaces, prefix='"
                   (:prefix resource) "'")
          response (-> {:status code :message msg}
                       u/json-response
                       (r/status code))]
      (throw (ex-info msg response)))))

(def validate-fn (u/create-validation-fn Attribute))
(defmethod crud/validate resource-uri
  [resource]
  (-> resource
      validate-fn
      validate-attribute-namespace))

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defn positive-biginteger
  [^bytes bytes]
  (BigInteger. 1 bytes))

(defn uri->id
  [^String uri]
  (if uri
    (try
      (-> uri
          (URI.)
          (.toASCIIString)
          (.getBytes (Charset/forName "US-ASCII"))
          (positive-biginteger)
          (.toString 16))
      (catch URISyntaxException _
        (throw (Exception. (str "invalid attribute URI: " uri)))))
    (throw (Exception. (str "attribute URI cannot be nil")))))

(defmethod crud/new-identifier resource-name
  [json resource-name]
  (let [new-id (str resource-url "/" (uri->id (str (:prefix json) ":" (:attr-name json))))]
    (assoc json :id new-id)))

(defmethod crud/add resource-name
  [request]
  (add-impl request))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(def edit-impl (std-crud/edit-fn resource-name))

(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))
