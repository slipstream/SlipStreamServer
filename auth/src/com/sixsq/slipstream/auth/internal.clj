(ns com.sixsq.slipstream.auth.internal
  (:refer-clojure :exclude [update])
  (:require
    [clojure.tools.logging :as log]
    [clojure.set :refer [rename-keys]]

    [com.sixsq.slipstream.auth.sign :as sg]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [buddy.core.codecs :as co]
    [clojure.string :as s]
    [buddy.core.hash :as ha])
  (:import (java.util Date TimeZone)
           (java.text SimpleDateFormat)))

(defn- extract-credentials
  [request]
  (let [username (->> request :params ((some-fn :username :user-name)))]
    {:username username
     :password (uh/param-value request :password)}))

(defn- log-result
  [credentials ok?]
  (log/info (str "'" (:username credentials) "' : "
                 (if ok? "login OK" "invalid password"))))

(defn- response-token-ok
  [token]
  (assoc
    (uh/response 200)
    :cookies
    {"com.sixsq.slipstream.cookie" {:value token, :path "/"}}))

(defn sha512
  "Encrypt secret exactly as done in SlipStream Java server."
  [secret]
  (-> (ha/sha512 secret)
      co/bytes->hex
      s/upper-case))

(defn valid?
  [credentials]
  (let [username            (:username credentials)
        password-credential (:password credentials)
        encrypted-in-db     (db/find-password-for-username username)]
    (and
      password-credential
      encrypted-in-db
      (= (sha512 password-credential) encrypted-in-db))))

(defn- adapt-credentials
  [{:keys [username] :as credentials}]
  (-> credentials
      (dissoc :password)
      (rename-keys {:username :com.sixsq.identifier})
      (merge {:com.sixsq.roles (db/find-roles-for-username username)})
      (merge {:exp (sg/expiry-timestamp)})))

(defn create-token
  ([credentials]
   (if (valid? credentials)
     [true {:token (sg/sign-claims (adapt-credentials credentials))}]
     [false {:message "Invalid credentials when creating token"}]))

  ([claims token]
   (log/debug "Will create token for claims=" claims)
   (try
     (sg/unsign-claims token)
     [true {:token (sg/sign-claims claims)}]
     (catch Exception e
       (log/error "exception in token creation " e)
       [false {:message (str "Invalid token when creating token: " e)}]))))

(defn login
  [request]

  (log/debug "Starting internal authentication.")
  (let [credentials (extract-credentials request)
        [ok? token] (create-token credentials)]
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
  (log/info "Logout internal authentication")
  (assoc
    (uh/response 200)
    :cookies
    {"com.sixsq.slipstream.cookie"
     {:value "INVALID", :path "/", :max-age 0, :expires (now-gmt)}}))
