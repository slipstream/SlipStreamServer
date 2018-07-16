(ns com.sixsq.slipstream.ssclj.resources.session.utils
  (:require
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.util.response :as r]
    [ring.util.codec :as codec]))


(defn cookie-name
  "Provides the name of the cookie based on the resource ID in the
   body of the response.  Currently this provides a fixed name to
   remain compatible with past implementations.

   FIXME: Update the implementation to use the session ID for the cookie name."
  [resource-id]
  ;; FIXME: Update the implementation to use the session ID for the cookie name.
  ;;(str "slipstream." (str/replace resource-id "/" "."))
  "com.sixsq.slipstream.cookie")


(defn validate-action-url-unencoded
  [base-uri session-id]
  (str base-uri session-id "/validate"))


(defn validate-action-url
  [base-uri session-id]
  (codec/url-encode (validate-action-url-unencoded base-uri session-id)))


(defn extract-session-id
  "Extracts the session identifier from a given URL."
  [uri]
  (second (re-matches #".*(session/[^/]+)/.*" uri)))


(defn extract-session-uuid
  "Extracts the session uuid from the session identifier."
  [session-id]
  (second (re-matches #"session/(.+)" session-id)))


(def internal-edit (std-crud/edit-fn p/resource-name))


(defn create-session
  "Creates a new session resource from the users credentials and the request
   header. The result contains the authentication method, the user's identifier,
   the client's IP address, and the virtual host being used. NOTE: The expiry
   is not included and MUST be added afterwards."
  [{:keys [href username redirectURI]} headers authn-method]
  (let [server (:slipstream-ssl-server-name headers)
        client-ip (:x-real-ip headers)]
    (crud/new-identifier
      (cond-> {:method          authn-method
               :sessionTemplate {:href href}}
              username (assoc :username username)
              server (assoc :server server)
              client-ip (assoc :clientIP client-ip)
              redirectURI (assoc :redirectURI redirectURI))
      p/resource-name)))


(defn retrieve-session-by-id
  "Retrieves a Session based on its identifier. Bypasses the authentication
   controls in the database CRUD layer by spoofing the session role."
  [session-id]
  (crud/retrieve-by-id session-id
                       {:user-name  "INTERNAL"
                        :user-roles [session-id]}))


(defn update-session
  "Updates the Session identified by the given identifier  Bypasses the
   authentication controls in the database CRUD layer by spoofing the
   session role."
  [session-id updated-session]
  (internal-edit {:user-name  "INTERNAL"
                  :user-roles [session-id]
                  :identity   {:current         "INTERNAL"
                               :authentications {"INTERNAL" {:identity "INTERNAL"
                                                             :roles    [session-id]}}}
                  :params     {:uuid (extract-session-uuid session-id)}
                  :body       updated-session}))


;; FIXME: Fix ugliness around needing to create ring requests with authentication!
(defn create-callback [baseURI session-id action]
  (let [callback-request {:params   {:resource-name callback/resource-url}
                          :body     {:action         action
                                     :targetResource {:href session-id}}
                          :identity {:current         "INTERNAL"
                                     :authentications {"INTERNAL" {:identity "INTERNAL"
                                                                   :roles    ["ADMIN"]}}}}
        {{:keys [resource-id]} :body status :status} (crud/add callback-request)]
    (if (= 201 status)
      (if-let [callback-resource (crud/set-operations (crud/retrieve-by-id-as-admin resource-id) {})]
        (if-let [validate-op (u/get-op callback-resource "execute")]
          (str baseURI validate-op)
          (let [msg "callback does not have execute operation"]
            (throw (ex-info msg (r/map-response msg 500 resource-id)))))
        (let [msg "cannot retrieve  session callback"]
          (throw (ex-info msg (r/map-response msg 500 resource-id)))))
      (let [msg "cannot create  session callback"]
        (throw (ex-info msg (r/map-response msg 500 session-id)))))))
