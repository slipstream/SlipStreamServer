(ns com.sixsq.slipstream.auth.utils.db
  (:require [clojure.tools.logging :as log]
            [korma.core :as kc]
            [korma.db :refer [defdb]]
            [com.sixsq.slipstream.auth.utils.config :as cf]))

(defn db-spec
  []
  (cf/property-value :auth-db))

(def init-db
  (delay
    (let [current-db-spec (db-spec)]
      (log/info (format "Creating korma database %s" current-db-spec))
      (defdb korma-auth-db current-db-spec))
      (log/info "Korma init done")
      (kc/defentity users (kc/table "USER") (kc/database korma-auth-db))
      (log/info "Korma Entities defined")))

(defn init
  []
  @init-db)

(defn find-usernames-by-email
  [email]
  (init)
  (->> (kc/select users
                  (kc/fields [:NAME])
                  (kc/where {:EMAIL email}))
       (map :NAME)))

(defn find-username-by-authn
  [authn-method authn-id]
  (init)
  (let [matched-users
        (kc/select users
                   (kc/fields [:NAME])
                   (kc/where (zipmap [:AUTHNMETHOD :AUTHNID] [authn-method authn-id])))]
    (if (> (count matched-users) 1)
      (throw (Exception. (str "There should be only one result for " authn-method "/" authn-id)))
      (-> (first matched-users)
          :NAME))))

(defn update-user-authn-info
  [slipstream-username authn-method authn-id]
  (init)
  (kc/update users
             (kc/set-fields (zipmap [:AUTHNMETHOD :AUTHNID] [authn-method authn-id]))
             (kc/where {:NAME slipstream-username}))
  slipstream-username)

(defn create-user
  [authn-method authn-login email]
  (init)
  (let [slipstream-username (str authn-method "-" authn-login)
        resourceuri (str "user/" slipstream-username)]
    (kc/insert users (kc/values
                       (zipmap ["RESOURCEURI" "DELETED" "JPAVERSION" "ISSUPERUSER"
                                "STATE" "NAME" "EMAIL" "AUTHNMETHOD" "AUTHNID"]
                               [resourceuri false 0 false
                                "ACTIVE" slipstream-username email authn-method authn-login])))
    slipstream-username))

(defn insert-user
  [name password email authn-method authn-id]
  (init)
  (kc/insert users (kc/values {:NAME         name
                               :PASSWORD     password
                               :EMAIL        email
                               :AUTHNMETHOD  authn-method
                               :AUTHNID      authn-id})))

(defn find-password-for-user-name
  [user-name]
  (init)
  (-> (kc/select users
                 (kc/fields [:PASSWORD])
                 (kc/where {:NAME user-name}))
      first
      :PASSWORD))