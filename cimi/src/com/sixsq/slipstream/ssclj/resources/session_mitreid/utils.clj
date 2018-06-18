(ns com.sixsq.slipstream.ssclj.resources.session-mitreid.utils
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.util.response :as r]))

(defn prefix
  [realm attr]
  (when (and realm attr)
    (str realm ":" attr)))

(defn extract-roles
  [{:keys [realm roles] :as claims}]
  (if (and (not (str/blank? realm)) roles)
    (->> roles
         (remove str/blank?)
         (map (partial prefix realm))
         vec)
    []))

(defn extract-entitlements
  [{:keys [realm entitlement] :as claims}]
  (if (and (not (str/blank? realm)) entitlement)
    (let [entitlement (if (instance? String entitlement) [entitlement] entitlement)]
      (->> entitlement
           (remove str/blank?)
           (map (partial prefix realm))
           vec))
    []))

(defn group-hierarchy
  [group]
  (if-not (str/blank? group)
    (let [terms (remove str/blank? (str/split group #"/"))]
      (doall
        (for [i (range 1 (inc (count terms)))]
          (str "/" (str/join "/" (take i terms))))))
    []))

(defn extract-groups
  [{:keys [realm groups] :as claims}]
  (if (and realm groups)
    (->> groups
         (mapcat group-hierarchy)
         (map (partial prefix realm))
         vec)
    []))


(def ^:const admin-opts {:user-name "INTERNAL", :user-roles ["ADMIN"]})

;; exceptions

(defn throw-no-username-or-email [username email redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "MITREid token is missing name/preferred_name (" username ") or email (" email ")") redirectURI))

(defn throw-no-matched-user [username email redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "Unable to match account to name/preferred_name (" username ") or email (" email ")") redirectURI))

;; general exceptions

(defn throw-bad-client-config [cfg-id redirectURI]
  (logu/log-error-and-throw-with-redirect 500 (str "missing or incorrect configuration (" cfg-id ") for MITREid authentication") redirectURI))

(defn throw-missing-mitreid-code [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "MITREid authentication callback request does not contain required code" redirectURI))

(defn throw-no-access-token [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "unable to retrieve MITREid access token" redirectURI))

(defn throw-no-subject [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "MITREid token is missing subject (sub) attribute") redirectURI))

(defn throw-invalid-access-code [msg redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "error when processing MITREid access token: " msg) redirectURI))

(defn throw-inactive-user [username redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "account is inactive (" username ")") redirectURI))

(defn throw-user-exists [username redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "account already exists (" username ")") redirectURI))

;; retrieval of configuration parameters

(defn config-params
  [redirectURI instance]
  (let [cfg-id (str "configuration/session-mitreid-" instance)
        opts {:user-name "INTERNAL" :user-roles ["ADMIN"]}] ;; FIXME: works around authn at DB interface level
    (try
      (let [{:keys [clientID clientSecret baseURL publicKey authorizeURL tokenURL]} (crud/retrieve-by-id cfg-id opts)]
        (if (or (and clientID baseURL publicKey) (and clientID clientSecret authorizeURL tokenURL publicKey))
          [clientID clientSecret baseURL publicKey authorizeURL tokenURL]
          (throw-bad-client-config cfg-id redirectURI)))
      (catch Exception _
        (throw-bad-client-config cfg-id redirectURI)))))

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
      (if-let [callback-resource (crud/set-operations (crud/retrieve-by-id resource-id admin-opts) {})]
        (if-let [validate-op (u/get-op callback-resource "execute")]
          (str baseURI validate-op)
          (let [msg "callback does not have execute operation"]
            (throw (ex-info msg (r/map-response msg 500 resource-id)))))
        (let [msg "cannot retrieve  session callback"]
          (throw (ex-info msg (r/map-response msg 500 resource-id)))))
      (let [msg "cannot create  session callback"]
        (throw (ex-info msg (r/map-response msg 500 session-id)))))))

(defn create-redirect-url
  "Generate a callback-url. By default, use authorizeURL if provided,
  otherwise fallback to base-url/auth"
  [base-url authorizeURL client-id callback-url]
  (let [url-params-format "?response_type=code&client_id=%s&redirect_uri=%s"
        base-redirect-url (or authorizeURL (str base-url "/auth"))]
    (str base-redirect-url (format url-params-format client-id callback-url))))
