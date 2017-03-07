(ns com.sixsq.slipstream.auth.internal
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [buddy.core.codecs :as co]
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
      str/upper-case))

(defn valid?
  [{:keys [username password] :as credentials}]
  (let [hashed-password (db/find-password-for-username username)]
    (and
      password
      hashed-password
      (= (sha512 password) hashed-password))))

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
        (log/info "successful internal login for" username)
        (response-ok cookie))
      (do
        (log/error "FAILED internal login for" username)
        (uh/response-forbidden)))))

(defn logout
  []
  (log/info "successful logout")
  (assoc
    (uh/response 200)
    :cookies (cookies/revoked-cookie "com.sixsq.slipstream.cookie")))
