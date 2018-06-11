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
        (map-slipstream-user! authn-method (first usernames-same-email) external-login)))))


(defn match-existing-external-user
  [authn-method external-login external-email]
  (log/debugf "Matching external user with method '%s', external-login '%s', and external-email '%s'"
              authn-method external-login external-email)
  (when-let [username-mapped (db/find-username-by-authn authn-method external-login)]
    [(mapped-user authn-method username-mapped) "/dashboard"]))


(defn sanitize-login-name
  "Replace characters not satisfying [a-zA-Z0-9_] with underscore"
  [s]
  (when s (str/replace s #"[^a-zA-Z0-9_-]" "_")))


(defn match-oidc-username
  [external-login]
  (log/debug "Matching via OIDC username" external-login)
  (let [username-by-authn (db/find-username-by-authn :oidc (sanitize-login-name external-login))
        username-by-name (db/get-active-user-by-name external-login)
        username-fallback (when username-by-name (:username (mapped-user :oidc username-by-name)))]
    (or username-by-authn username-fallback)))


(defn create-user-when-missing!
  [authn-method {:keys [external-login external-email fail-on-existing?] :as external-record}]
  (let [username-by-authn (db/find-username-by-authn authn-method (sanitize-login-name external-login))
        username (u/random-uuid)]
    (if (and username-by-authn (not fail-on-existing?))
      username-by-authn
      (when-not username-by-authn (if-not
                                    (or (db/user-exists? (or (sanitize-login-name external-login) username))
                                        (db/external-identity-exists? authn-method (or (sanitize-login-name external-login) username)))
                                    (create-slipstream-user! (assoc external-record
                                                               :authn-login username
                                                               :external-login (sanitize-login-name external-login)
                                                               :email external-email
                                                               :authn-method (name authn-method)))

                                    (when-not fail-on-existing?
                                      (or (sanitize-login-name external-login) username)))))))
