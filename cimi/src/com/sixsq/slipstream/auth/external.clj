(ns com.sixsq.slipstream.auth.external
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.auth.utils.http :as uh]))

(defn- mapped-user
  [authn-method username]
  (log/info (str "External (" authn-method ") user '" username "' already mapped => login ok."))
  username)

(defn- map-slipstream-user!
  [authn-method slipstream-username external-login]
  (log/info (str "Mapping external (" authn-method ") user '" external-login
                 "' to existing SlipStream user '" slipstream-username "'"))
  (db/update-user-authn-info authn-method slipstream-username external-login))

(defn- create-slipstream-user!
  ([authn-method external-login external-email]
   (log/info (str "Creating new SlipStream user with external (" authn-method ") user '" external-login "'"))
   (let [user-name (db/create-user! authn-method external-login external-email)]
     (when user-name
       (db/create-user-params! user-name))
     user-name))
  ([{:keys [authn-login] :as user-record}]
   (log/info (str "Creating new SlipStream user '" authn-login "'"))
   (let [user-name (db/create-user! user-record)]
     (when user-name
       (db/create-user-params! user-name))
     user-name)))

(defn match-external-user!
  [authn-method external-login external-email]
  (if-let [username-mapped (db/find-username-by-authn authn-method external-login)]
    [(mapped-user authn-method username-mapped) "/dashboard"]
    (let [usernames-same-email (db/find-usernames-by-email external-email)]
      (if (empty? usernames-same-email)
        (let [name-new-user (create-slipstream-user! authn-method external-login external-email)]
          [name-new-user (format "/user/%s?edit=true" name-new-user)])
        [(map-slipstream-user! authn-method (first usernames-same-email) external-login) "/dashboard"]))))

(defn sanitize-login-name
  "Replace characters not satisfying [a-zA-Z0-9_] with underscore"
  [s]
  (when s (str/replace s #"[^a-zA-Z0-9_-]" "_")))

(defn create-user-when-missing!
  [{:keys [authn-login fail-on-existing?] :as user-record}]
  (let [username (sanitize-login-name authn-login)]
    (if-not (db/user-exists? username)
     (create-slipstream-user! (assoc user-record :authn-login username))
     (when-not fail-on-existing?
       username))))

(defn redirect-with-matched-user
  [authn-method external-login external-email redirect-server]
  (if (and (not-empty external-login) (not-empty external-email))
    (let [[matched-user redirect-url] (match-external-user! authn-method (sanitize-login-name external-login) external-email)
          claims {:username matched-user
                  :roles    (db/find-roles-for-username matched-user)}]
      (assoc
        (uh/response-redirect (str redirect-server redirect-url))
        :cookies (cookies/claims-cookie claims "com.sixsq.slipstream.cookie")))
    (uh/response-redirect (str redirect-server "/login?flash-now-warning=auth-failed"))))

