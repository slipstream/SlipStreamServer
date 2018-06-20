(ns com.sixsq.slipstream.ssclj.resources.email
  "This resource corresponds to an email address. The resource contains only
   the common attributes, a syntactically valid email address, and a boolean
   flag that indicates if the email address has been validated.

   When the address has not been validated, a 'validate' action is provided.
   This will send an email to the user with a callback URL to validate the
   email address. When the callback is triggered, the validated? flag is set to
   true."
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.email.utils :as email-utils]
    [com.sixsq.slipstream.ssclj.resources.spec.email :as email]
    [com.sixsq.slipstream.util.response :as r]
    [superstring.core :as str]))

(def ^:const resource-name "Email")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "EmailCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})


;;
;; multimethod for ACLs
;;

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl (dissoc resource :acl) request))

;;
;; "Implementations" of multimethod declared in crud namespace
;;

;; resource identifier is the MD5 checksum of the email address
(defmethod crud/new-identifier resource-name
  [resource resource-name]
  (if-let [new-id (some-> resource :address email-utils/md5)]
    (assoc resource :id (str (u/de-camelcase resource-name) "/" new-id))))


(def validate-fn (u/create-spec-validation-fn ::email/email))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))


(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [request]
  (add-impl (assoc-in request [:body :validated?] false)))


(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))


(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

;;
;; available operations; disallows editing of resource, adds validate action for unvalidated emails
;;
(defmethod crud/set-operations resource-uri
  [{:keys [validated?] :as resource} request]
  (try
    (a/can-modify? resource request)
    (let [href (:id resource)
          ^String resourceURI (:resourceURI resource)
          ops (if (.endsWith resourceURI "Collection")
                [{:rel (:add c/action-uri) :href href}]
                (cond-> [{:rel (:delete c/action-uri) :href href}]
                        (not validated?) (conj {:rel (:validate c/action-uri) :href (str href "/validate")})))]
      (assoc resource :operations ops))
    (catch Exception _
      (dissoc resource :operations))))

;;
;; collection
;;
(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
  [request]
  (query-impl request))


;;
;; actions
;;
(defmethod crud/do-action [resource-url "validate"]
  [{{uuid :uuid} :params baseURI :baseURI}]
  (let [id (str resource-url "/" uuid)]
    (when-let [{:keys [address validated?]} (crud/retrieve-by-id-as-admin id)]
      (if-not validated?
        (try
          (-> (email-utils/create-callback id baseURI)
              (email-utils/send-validation-email address))
          (r/map-response "check your mailbox for a validation message" 202)
          (catch Exception e
            (.printStackTrace e)))
        (throw (r/ex-bad-request "email address is already validated"))))))


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize resource-url ::email/email))
