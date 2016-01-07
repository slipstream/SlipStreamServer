(ns com.sixsq.slipstream.auth.internal-authentication
  (:refer-clojure :exclude [update])
  (:require
    [clojure.tools.logging                            :as log]
    [clojure.set                                      :refer [rename-keys]]

    [com.sixsq.slipstream.auth.sign                   :as sg]
    [com.sixsq.slipstream.auth.utils.db               :as db]))


;; TODO : Currently unused as DB insertion is done by Java server
(defn add-user!
  [user]
  (db/init)
  (log/info "Will add user " (:user-name user))
  (db/insert-user (:user-name user)
                  (sg/sha512 (:password user))
                  (:email user)
                  (:authn-method user)
                  (:authn-id user)))

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
      (sg/check-token token)
      [true {:token (sg/sign-claims claims)}]
      (catch Exception e
        (log/error "exception in token creation " e)
        [false {:message (str "Invalid token when creating token: " e)}]))))