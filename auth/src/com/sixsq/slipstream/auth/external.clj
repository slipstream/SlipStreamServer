(ns com.sixsq.slipstream.auth.external
  (:require [clojure.tools.logging :as log]
            [com.sixsq.slipstream.auth.utils.db :as db]
            [com.sixsq.slipstream.auth.sign :as sg]
            [com.sixsq.slipstream.auth.utils.http :as uh]))

(defn- mapped-user
  [authn-method user-name]
  (log/info (str "External (" authn-method ") user '" user-name "' already mapped => login ok."))
  user-name)

(defn- map-slipstream-user!
  [authn-method slipstream-username external-login]
  (log/info (str "Mapping external (" authn-method ") user '" external-login
                 "' to existing SlipStream user '" slipstream-username "'"))
  (db/update-user-authn-info authn-method slipstream-username external-login))

(defn- create-slipstream-user!
  [authn-method external-login external-email]
  (log/info (str "Creating new SlipStream user with external user '" external-login "'"))
  (db/create-user! authn-method external-login external-email))

(defn- match-external-user!
  [authn-method external-login external-email]
  (if-let [user-name-mapped (db/find-username-by-authn authn-method external-login)]
    (mapped-user authn-method user-name-mapped)
    (let [user-names-same-email (db/find-usernames-by-email external-email)]
      (if (empty? user-names-same-email)
        (create-slipstream-user! authn-method external-login external-email)
        (map-slipstream-user! authn-method (first user-names-same-email) external-login)))))

(defn redirect-with-matched-user
  [authn-method external-login external-email redirect-server]

  (let [matched-user (match-external-user! authn-method external-login external-email)
        token (sg/sign-claims {:com.sixsq.identifier matched-user
                               :exp                  (sg/expiry-timestamp)})]

    (-> (uh/response-redirect (str redirect-server "/dashboard"))
        (assoc :cookies {"com.sixsq.slipstream.cookie" {:value {:token token}
                                                        :path  "/"}}))))