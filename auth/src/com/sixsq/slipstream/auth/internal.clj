(ns com.sixsq.slipstream.auth.internal
  (:refer-clojure :exclude [update])
  (:require
    [clojure.tools.logging :as log]
    [clojure.set :refer [rename-keys]]

    [com.sixsq.slipstream.auth.sign :as sg]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.http :as uh])
  (:import (java.util Date TimeZone)
           (java.text SimpleDateFormat)))

(defn- extract-credentials
  [request]
  (uh/select-in-params request [:user-name :password]))

(defn- log-result
  [credentials ok?]
  (log/info (str "'" (:user-name credentials) "' : "
                 (if ok? "login OK" "invalid password"))))

(defn- response-token-ok
  [token]
  (-> (uh/response 200)
      (assoc :cookies {"com.sixsq.slipstream.cookie" {:value token
                                                      :path  "/"}})))

(defn valid?
  [credentials]
  (let [user-name           (:user-name credentials)
        password-credential (:password credentials)
        encrypted-in-db     (db/find-password-for-user-name user-name)]
    (and
      password-credential
      encrypted-in-db
      (= (sg/sha512 password-credential) encrypted-in-db))))

(defn- adapt-credentials
  [credentials]
  (-> credentials
      (dissoc :password)
      (rename-keys {:user-name :com.sixsq.identifier})
      (merge {:exp (sg/expiry-timestamp)})))

(defn create-token
  ([credentials]
  (if (valid? credentials)
    [true  {:token (sg/sign-claims (adapt-credentials credentials))}]
    [false {:message "Invalid credentials when creating token"}]))

  ([claims token]
   (log/info "Will create token for claims=" claims)
   (try
      (sg/unsign-claims token)
      [true {:token (sg/sign-claims claims)}]
      (catch Exception e
        (log/error "exception in token creation " e)
        [false {:message (str "Invalid token when creating token: " e)}]))))

(defn login
  [request]

  (log/info "Internal authentication.")
  (let [credentials  (extract-credentials request)
        [ok? token]  (create-token credentials)]
    (log-result credentials ok?)
    (if ok?
      (response-token-ok token)
      (uh/response-forbidden))))

(def ^:private sdf
  (doto (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss z")
        (.setTimeZone (TimeZone/getTimeZone "GMT"))))

(defn now-gmt
  []
  (.format sdf (Date.)))

(defn logout
  []
  (log/info "Logout Internal authentication")
  (-> (uh/response 200)
      (assoc :cookies {"com.sixsq.slipstream.cookie" {:value   "INVALID"
                                                      :path    "/"
                                                      :max-age 0
                                                      :expires (now-gmt)}})))
