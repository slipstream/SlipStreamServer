(ns com.sixsq.slipstream.auth.external
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))


(defn- mapped-user
  [authn-method username]
  (log/infof "External (%s) user '%s' already mapped => login ok." authn-method username)
  username)


(defn- map-slipstream-user!
  [authn-method slipstream-username external-login]
  (log/infof "Mapping external (%s) user '%s' to existing SlipStream user '%s'"
             authn-method external-login slipstream-username)
  (db/update-user-authn-info authn-method slipstream-username external-login))


(defn- create-slipstream-user!
  ([authn-method external-login external-email]
   (log/infof "Creating new SlipStream user with external (%s) user '%s'"
              authn-method external-login)
   (let [user-name (db/create-user! authn-method external-login external-email)]
     (when user-name
       (db/create-user-params! user-name))
     user-name))

  ([{:keys [authn-login] :as user-record}]
   (log/infof "Creating new SlipStream user '%'" authn-login)
   (let [user-name (db/create-user! user-record)]
     (when user-name
       (db/create-user-params! user-name))
     user-name)))


(defn match-external-user!
  [authn-method external-login external-email]
  (if-let [username-mapped (db/find-username-by-authn authn-method external-login)]
    (mapped-user authn-method username-mapped)
    (let [usernames-same-email (db/find-usernames-by-email external-email)]
      (if (empty? usernames-same-email)
        (let [name-new-user (create-slipstream-user! authn-method external-login external-email)]
          [name-new-user (format "/user/%s?edit=true" name-new-user)])
        (map-slipstream-user! authn-method (first usernames-same-email) external-login) ))))


(defn match-existing-external-user
  [authn-method external-login external-email]
  (log/debugf "Matching external user with method '%s', external-login '%s', and external-email '%s'"
              authn-method external-login external-email)
  (when-let [username-mapped (db/find-username-by-authn authn-method external-login)]
    [(mapped-user authn-method username-mapped) "/dashboard"]))

(defn match-oidc-username
  [external-login]
  (log/debug "Matching via username" external-login)
  (when-let [username-mapped (db/get-active-user-by-name external-login)]
    (:username (mapped-user :oidc username-mapped))))


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


(defn create-github-user-when-missing!
  [{:keys [github-login github-email fail-on-existing?] :as github-record}]
  (let [username (db/find-username-by-authn :github github-login)]
    (when-not username (create-user-when-missing! {:authn-login (u/random-uuid)
                                                   :email github-email
                                                   :external-login github-login
                                                   :authn-method "github"
                                                   :fail-on-existing? fail-on-existing?}))))
