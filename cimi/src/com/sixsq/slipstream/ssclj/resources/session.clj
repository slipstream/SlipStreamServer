(ns com.sixsq.slipstream.ssclj.resources.session
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.db.filter.parser :as parser]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]))

(def ^:const resource-tag :sessions)

(def ^:const resource-name "Session")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "SessionCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "ANON"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; validate subclasses of sessions
;;

(defmulti validate-subtype
          :method)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown Session type: '" (:method resource) "'") resource)))

(defmethod crud/validate resource-uri
  [resource]
  (validate-subtype resource))

;;
;; validate create requests for subclasses of sessions
;;

(defn dispatch-on-authn-method [resource]
  (get-in resource [:sessionTemplate :method]))

(defmulti create-validate-subtype dispatch-on-authn-method)

(defmethod create-validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown Session create type: " (dispatch-on-authn-method resource) resource) resource)))

(defmethod crud/validate create-uri
  [resource]
  (create-validate-subtype resource))

;;
;; multimethod for ACLs
;;

(defn create-acl
  [id]
  {:owner {:principal id
           :type      "ROLE"}
   :rules [{:principal "ADMIN"
            :type      "ROLE"
            :right     "VIEW"}]})

(defmethod crud/add-acl resource-uri
  [{:keys [id acl] :as resource} request]
  (assoc
    resource
    :acl
    (or acl (create-acl id))))

(defn dispatch-conversion
  "Dispatches on the Session authentication method for multimethods
   that take the resource and request as arguments."
  [resource _]
  (:method resource))

(defn standard-session-operations
  "Provides a list of the standard session operations, depending
   on the user's authentication and whether this is a Session or
   a SessionCollection."
  [{:keys [id resourceURI] :as resource} request]
  (try
    (a/can-modify? resource request)
    (if (.endsWith resourceURI "Collection")
      [{:rel (:add c/action-uri) :href id}]
      [{:rel (:delete c/action-uri) :href id}])
    (catch Exception _
      nil)))

;; Sets the operations for the given resources.  This is a
;; multi-method because different types of session resources
;; may require different operations, for example, a 'validation'
;; callback.
(defmulti set-session-operations dispatch-conversion)

;; Default implementation adds the standard session operations
;; by ALWAYS replacing the :operations value.  If there are no
;; operations, the key is removed from the resource.
(defmethod set-session-operations :default
  [resource request]
  (let [ops (standard-session-operations resource request)]
    (cond-> (dissoc resource :operations)
            (seq ops) (assoc :operations ops))))

;; Just triggers the Session-level multimethod for adding operations
;; to the Session resource.
(defmethod crud/set-operations resource-uri
  [resource request]
  (set-session-operations resource request))

;;
;; template processing
;;
;; The concrete implementation of this method MUST return a two-element
;; tuple containing a response fragment and the created session resource.
;; The response fragment will be merged with the 'add-impl' function
;; response and should be used to override the return status (e.g. to
;; instead provide a redirect) and to set a cookie header.
;;

(defmulti tpl->session dispatch-conversion)

;; All concrete session types MUST provide an implementation of this
;; multimethod. The default implementation will throw an 'internal
;; server error' exception.
;;
(defmethod tpl->session :default
  [resource request]
  [{:status 500, :message "invalid session resource implementation"} nil])

;;
;; CRUD operations
;;

(defn add-impl [{:keys [id body] :as request}]
  (a/can-modify? {:acl collection-acl} request)
  (db/add
    resource-name
    (-> body
        u/strip-service-attrs
        (assoc :id id)
        (assoc :resourceURI resource-uri)
        u/update-timestamps
        (crud/add-acl request)
        crud/validate)
    {}))

;; requires a SessionTemplate to create new Session
(defmethod crud/add resource-name
  [{:keys [body form-params headers] :as request}]

  (let [idmap {:identity (:identity request)}
        body (if (u/is-form? headers) (u/convert-form :sessionTemplate form-params) body)
        desc-attrs (u/select-desc-keys body)
        [cookie-header {:keys [id] :as body}] (-> body
                                                  (assoc :resourceURI create-uri)
                                                  (std-crud/resolve-hrefs idmap true)
                                                  (update-in [:sessionTemplate] merge desc-attrs) ;; validate desc attrs
                                                  (crud/validate)
                                                  (:sessionTemplate)
                                                  (tpl->session request))]
    (-> request
        (assoc :id id :body (merge body desc-attrs))
        add-impl
        (merge cookie-header))))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

;; FIXME: Copied to avoid dependency cycle.
(defn cookie-name
  "Provides the name of the cookie based on the resource ID in the
   body of the response.  Currently this provides a fixed name to
   remain compatible with past implementations.

   FIXME: Update the implementation to use the session ID for the cookie name."
  [resource-id]
  ;; FIXME: Update the implementation to use the session ID for the cookie name.
  ;;(str "slipstream." (str/replace resource-id "/" "."))
  "com.sixsq.slipstream.cookie")

(defn delete-cookie [{:keys [status] :as response}]
  (if (= status 200)
    {:cookies (cookies/revoked-cookie (cookie-name (-> response :body :resource-id)))}
    {}))

(defmethod crud/delete resource-name
  [request]
  (let [response (delete-impl request)
        cookies (delete-cookie response)]
    (merge response cookies)))

(defn add-session-filter [{{:keys [session]} :sixsq.slipstream.authn/claims :as request}]
  (->> (or session "")
       (format "id='%s'")
       (parser/parse-cimi-filter)
       (assoc-in request [:cimi-params :filter])))

(defn query-wrapper
  "wraps the standard query function to always include a filter based on the session"
  [query-fn]
  (fn [request]
    (query-fn (add-session-filter request))))

(def query-impl (query-wrapper (std-crud/query-fn resource-name collection-acl collection-uri resource-tag)))

(defmethod crud/query resource-name
  [request]
  (query-impl request))

;;
;; actions may be needed by certain authentication methods (notably external
;; methods like GitHub and OpenID Connect) to validate a given session
;;

(defmulti validate-callback dispatch-conversion)

(defmethod validate-callback :default
  [resource request]
  (log-util/log-and-throw 400 (str "error executing validation callback: '" (dispatch-conversion resource request) "'")))

(defmethod crud/do-action [resource-url "validate"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (validate-callback (crud/retrieve-by-id-as-admin id) request))
    (catch Exception e
      (or (ex-data e) (throw e)))))


;;
;; initialization: no schema for this parent resource
;;
(defn initialize
  []
  (std-crud/initialize resource-url nil))
