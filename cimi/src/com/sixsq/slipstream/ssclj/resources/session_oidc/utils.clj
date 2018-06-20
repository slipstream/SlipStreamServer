(ns com.sixsq.slipstream.ssclj.resources.session-oidc.utils
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]
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


;; exceptions

(defn throw-no-username-or-email [username email redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "OIDC/MITREid token is missing name/preferred_name (" username ") or email (" email ")") redirectURI))

(defn throw-no-matched-user [username email redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "Unable to match account to name/preferred_name (" username ") or email (" email ")") redirectURI))

;; general exceptions

(defn throw-bad-client-config [cfg-id redirectURI]
  (logu/log-error-and-throw-with-redirect 500 (str "missing or incorrect configuration (" cfg-id ") for OIDC/MITREid authentication") redirectURI))

(defn throw-missing-code [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "OIDC/MITREid authentication callback request does not contain required code" redirectURI))

(defn throw-no-access-token [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "unable to retrieve OIDC/MITREid access token" redirectURI))

(defn throw-no-subject [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "OIDC/MITREid token is missing subject (sub) attribute") redirectURI))

(defn throw-invalid-access-code [msg redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "error when processing OIDC/MITREid access token: " msg) redirectURI))

(defn throw-inactive-user [username redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "account is inactive (" username ")") redirectURI))

(defn throw-user-exists [username redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "account already exists (" username ")") redirectURI))



(defn valid-mitreid-config?
  [{:keys [clientID clientSecret publicKey authorizeURL tokenURL userProfileURL] :as config}]
  (and clientID clientSecret authorizeURL tokenURL userProfileURL publicKey))

(defn valid-oidc-config?
  "An OIDC config without clientSecret is valid (e.g KeyCloak)"
  [{:keys [clientID publicKey authorizeURL tokenURL] :as config}]
  (and clientID authorizeURL tokenURL publicKey))

;; retrieval of configuration parameters

(defn config-oidc-params
  [redirectURI instance]
  (let [cfg-id (str "configuration/session-oidc-" instance)]
    (try
      (let [oidc-config (crud/retrieve-by-id-as-admin cfg-id)
            {:keys [clientID clientSecret publicKey authorizeURL tokenURL]} oidc-config]
        (if (valid-oidc-config? oidc-config)
          [clientID clientSecret publicKey authorizeURL tokenURL]
          (throw-bad-client-config cfg-id redirectURI)))
      (catch Exception _
        (throw-bad-client-config cfg-id redirectURI)))))

(defn config-mitreid-params
  [redirectURI instance]
  (let [cfg-id (str "configuration/session-mitreid-" instance)]
    (try
      (let [mitreid-config (crud/retrieve-by-id-as-admin cfg-id)
            {:keys [clientID clientSecret publicKey authorizeURL tokenURL userProfileURL]} mitreid-config]
        (if (valid-mitreid-config? mitreid-config)
          [clientID clientSecret publicKey authorizeURL tokenURL userProfileURL]
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
      (if-let [callback-resource (crud/set-operations (crud/retrieve-by-id-as-admin resource-id) {})]
        (if-let [validate-op (u/get-op callback-resource "execute")]
          (str baseURI validate-op)
          (let [msg "callback does not have execute operation"]
            (throw (ex-info msg (r/map-response msg 500 resource-id)))))
        (let [msg "cannot retrieve  session callback"]
          (throw (ex-info msg (r/map-response msg 500 resource-id)))))
      (let [msg "cannot create  session callback"]
        (throw (ex-info msg (r/map-response msg 500 session-id)))))))

(defn create-redirect-url
  "Generate a redirect-url from the provided authorizeURL"
  [authorizeURL client-id callback-url]
  (let [url-params-format "?response_type=code&client_id=%s&redirect_uri=%s"]
    (str authorizeURL (format url-params-format client-id callback-url))))

(defn get-mitreid-userinfo
  [userProfileURL access_token]
  (-> (http/get userProfileURL
                {:headers      {"Accept" "application/json"}
                 :query-params {::access_token access_token}})
      :body
      (json/read-str :key-fn keyword)))
