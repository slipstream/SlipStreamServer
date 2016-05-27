(ns com.sixsq.slipstream.auth.external
  (:require [clojure.tools.logging :as log]
            [superstring.core :as s]
            [com.sixsq.slipstream.auth.utils.db :as db]
            [com.sixsq.slipstream.auth.sign :as sg]
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
  [authn-method external-login external-email]
  (log/info (str "Creating new SlipStream user with external (" authn-method ") user '" external-login "'"))
  (db/create-user! authn-method external-login external-email))

(defn match-external-user!
  [authn-method external-login external-email]
  (if-let [username-mapped (db/find-username-by-authn authn-method external-login)]
    [(mapped-user authn-method username-mapped) "/dashboard"]
    (let [usernames-same-email (db/find-usernames-by-email external-email)]
      (if (empty? usernames-same-email)
        (let [name-new-user (create-slipstream-user! authn-method external-login external-email)]
          [name-new-user (format "/user/%s?edit=true" name-new-user)])
        [(map-slipstream-user! authn-method (first usernames-same-email) external-login) "/dashboard"]))))

(defn redirect-with-matched-user
  [authn-method external-login external-email redirect-server]
  (if (and (not-empty external-login) (not-empty external-email))
    (let [[matched-user redirect-url] (match-external-user! authn-method external-login external-email)
          token (sg/sign-claims {:com.sixsq.identifier matched-user
                                 :com.sixsq.roles      (db/find-roles-for-username matched-user)
                                 :exp                  (sg/expiry-timestamp)})]

      (assoc
        (uh/response-redirect (str redirect-server redirect-url))
        :cookies {"com.sixsq.slipstream.cookie" {:value {:token token}
                                                 :path  "/"}}))
    (uh/response-redirect (str redirect-server "/login?flash-now-warning=auth-failed"))))

(defn sanitize-login-name
  "Replace characters not satisfying [a-zA-Z0-9_] with underscore"
  [s]
  (when s (s/replace s #"[^a-zA-Z0-9_]" "_")))
