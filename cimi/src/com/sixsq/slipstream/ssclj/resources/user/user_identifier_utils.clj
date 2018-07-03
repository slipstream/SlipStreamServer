(ns com.sixsq.slipstream.ssclj.resources.user.user-identifier-utils
  (:require [clojure.string :as str]
            [com.sixsq.slipstream.db.filter.parser :as parser]
            [com.sixsq.slipstream.db.impl :as db]
            [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
            [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:private active-user-filter "(state='ACTIVE')")

(defn generate-identifier
  ([authn-method external-login]
   (generate-identifier authn-method external-login nil))
  ([authn-method external-login instance]
   (str (or instance (name authn-method)) ":" external-login)))


(defn add-user-identifier!
  [username authn-method external-login instance]
  (let [user-id (str "user/" username)
        identifier (generate-identifier authn-method external-login instance)]
    (crud/add
      {:identity     {:current "internal"
                      :authentications
                               {"internal" {:roles #{"ADMIN"}, :identity "internal"}}}
       :sixsq.slipstream.authn/claims
                     {:username "internal", :roles "ADMIN"}
       :params       {:resource-name "user-identifier"}
       :route-params {:resource-name "user-identifier"}
       :user-roles   #{"USER"}
       :body         {:identifier identifier
                      :user       {:href user-id}}})))

(defn- to-am-kw
  [authn-method]
  (keyword (str (name authn-method) "login")))

(defn find-username-by-identifier
  [authn-method instance external-login]
  (let [identifier (generate-identifier authn-method external-login instance)
        user-identifier-record (try
                                 (crud/retrieve-by-id (str "user-identifier/" (u/md5 identifier)))
                                 (catch Exception _
                                   nil))
        filter-str-fallback (format "%s='%s' and %s" (name (to-am-kw authn-method)) external-login active-user-filter)
        create-filter (fn [filter-string] {:filter (parser/parse-cimi-filter filter-string)})
        filter-fallback (create-filter filter-str-fallback)
        query-users-fallback (fn [f] (try
                                       (second (db/query "user" {:cimi-params f
                                                                 :user-roles  ["ADMIN"]}))
                                       (catch Exception _ [])))
        matched-users-fallback (query-users-fallback filter-fallback)
        ignore-user-when-deleted (fn [user-id]
                                   (when-not (= "DELETED" (-> (crud/retrieve-by-id user-id)
                                                              :state))
                                     user-id))
        get-user (fn [users] (:username (first users)))
        throw-ex (fn [users] (throw (Exception. (str "There should be only one result for "
                                                     external-login ". But found " (count users)))))]
    (cond
      user-identifier-record (some-> user-identifier-record
                                     :user
                                     :href
                                     (ignore-user-when-deleted)
                                     (str/split #"/")
                                     second)
      (= (count matched-users-fallback) 1) (get-user matched-users-fallback)
      (> (count matched-users-fallback) 1) (throw-ex matched-users-fallback))))

(defn external-identity-exists?
  [authn-method external-login]
  (let [identifier (generate-identifier authn-method external-login)
        user-identifier-record (try
                                 (crud/retrieve-by-id (str "user-identifier/" (u/md5 identifier)))
                                 (catch Exception _
                                   nil))]
    (not (nil? user-identifier-record))))

(defn find-identities-by-user
  [user-id]
  (let [filter-str (format "user/href='%s'" user-id)
        create-filter (fn [filter-string] {:filter (parser/parse-cimi-filter filter-string)})
        filter (create-filter filter-str)
        query-user-identities (fn [f] (try
                                        (second (db/query "UserIdentifier" {:cimi-params f
                                                                            :user-roles  ["ADMIN"]}))
                                        (catch Exception _ [])))]

    (query-user-identities filter)))
