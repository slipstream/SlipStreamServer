(ns com.sixsq.slipstream.auth.internal
  (:refer-clojure :exclude [update])
  (:require
    [clojure.tools.logging :as log]
    [clojure.set :refer [rename-keys]]

    [com.sixsq.slipstream.auth.sign :as sg]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [buddy.core.codecs :as co]
    [clojure.string :as str]
    [buddy.core.hash :as ha]))

(defn- extract-credentials
  [request]
  ;; FIXME: Remove :user-name!
  {:username (->> request :params ((some-fn :username :user-name)))
   :password (uh/param-value request :password)})

(defn sha512
  "Hash secret exactly as done in SlipStream Java server."
  [secret]
  (when secret
    (-> (ha/sha512 secret)
        co/bytes->hex
        str/upper-case)))

(defn valid?
  [{:keys [username password]}]
  (let [db-password-hash (db/find-password-for-username username)]
    (and
      password
      db-password-hash
      (= (sha512 password) db-password-hash))))

(defn create-claims
  [username]
  {:com.sixsq.identifier username
   :com.sixsq.roles      (db/find-roles-for-username username)})

#_(defn- adapt-credentials
    [{:keys [username] :as credentials}]
    (-> credentials
        (dissoc :password)
        (rename-keys {:username :com.sixsq.identifier})
        (merge {:com.sixsq.roles (db/find-roles-for-username username)})
        (merge {:exp (sg/expiry-timestamp)})))

#_(defn create-token
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
  (let [{:keys [username] :as credentials} (extract-credentials request)]
    (if (valid? credentials)
      (do
        (log/info "successful login for" username)
        (assoc
          (uh/response 200)
          :cookies (cookies/claims-cookie (create-claims username) "com.sixsq.slipstream.cookie")))
      (do
        (log/warn "failed login attempt for" username)
        (uh/response-forbidden)))))                         ;; FIXME: Returns 401, but should be 403.

(defn logout
  []
  (log/info "sending logout cookie")
  (assoc
    (uh/response 200)
    :cookies (cookies/revoked-cookie "com.sixsq.slipstream.cookie")))
