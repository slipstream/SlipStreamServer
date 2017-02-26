(ns com.sixsq.slipstream.auth.internal
  (:refer-clojure :exclude [update])
  (:require
    [clojure.tools.logging :as log]
    [clojure.set :refer [rename-keys]]

    [com.sixsq.slipstream.auth.sign :as sg]
    [com.sixsq.slipstream.auth.utils.timestamp :as ts]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [buddy.core.codecs :as co]
    [clojure.string :as s]
    [buddy.core.hash :as ha]))

(defn extract-credentials
  [request]
  (let [username (->> request :params ((some-fn :username :user-name)))]
    {:username username
     :password (uh/param-value request :password)}))

(defn response-ok
  [cookie]
  (assoc
    (uh/response 200)
    :cookies cookie))

(defn sha512
  "Encrypt secret exactly as done in SlipStream Java server."
  [secret]
  (-> (ha/sha512 secret)
      co/bytes->hex
      s/upper-case))

(defn valid?
  [{:keys [username password] :as credentials}]
  (let [hashed-password (db/find-password-for-username username)]
    (and
      password
      hashed-password
      (= (sha512 password) hashed-password))))

#_(defn create-token
  ([credentials]
   (if (valid? credentials)
     [true {:token (sg/sign-claims (create-claims credentials))}]
     [false {:message "Invalid credentials when creating token"}]))

  ([claims token]
   (log/debug "Will create token for claims=" claims)
   (try
     (sg/unsign-claims token)
     [true {:token (sg/sign-claims claims)}]
     (catch Exception e
       (log/error "exception in token creation " e)
       [false {:message (str "Invalid token when creating token: " e)}]))))

(defn create-claims
  [{:keys [username]}]
  {:com.sixsq.identifier username
   :com.sixsq.roles (db/find-roles-for-username username)})

(defn create-cookie
  [claims]
  (cookies/claims-cookie claims "com.sixsq.slipstream.cookie"))

(defn login
  [request]
  (let [{:keys [username password] :as credentials} (extract-credentials request)
        claims (create-claims credentials)]
    (log/debug "starting internal authentication for" username)
    (if (valid? credentials)
      (let [cookie (create-cookie claims)]
        (log/info "successful login for" username)
        (response-ok cookie))
      (do
        (log/error "FAILED login for" username)
        (uh/response-forbidden)))))

(defn logout
  []
  (log/info "Logout internal authentication")
  (assoc
    (uh/response 200)
    :cookies (cookies/revoked-cookie "com.sixsq.slipstream.cookie")))
