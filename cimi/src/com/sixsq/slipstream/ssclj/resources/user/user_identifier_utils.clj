(ns com.sixsq.slipstream.ssclj.resources.user.user-identifier-utils
  (:require
    [clojure.string :as str]
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


(defn external-identity-exists?
  ([authn-method external-login]
   (let [identifier (generate-identifier authn-method external-login)
         user-identifier-record (try
                                  (crud/retrieve-by-id (str "user-identifier/" (u/md5 identifier)))
                                  (catch Exception _
                                    nil))]
     (not (nil? user-identifier-record))))
  ([authn-method external-login instance]
   (let [identifier (generate-identifier authn-method external-login instance)
         user-identifier-record (try
                                  (crud/retrieve-by-id (str "user-identifier/" (u/md5 identifier)))
                                  (catch Exception _
                                    nil))]
     (not (nil? user-identifier-record)))))


(defn sanitize-login-name
  "Replace characters not satisfying [a-zA-Z0-9_] with underscore"
  [s]
  (when s (str/replace s #"[^a-zA-Z0-9_-]" "_")))


(defn update-sanitized-identifiers!
  "In case the database contains the sanitized version of the User identifier (e.g for OIDC)
  delete the record and create a new one from the original ('unsanitized') version of the external login"
  [username authn-method instance external-login]
  (let [sanitized-login (sanitize-login-name external-login)
        exist-unsanitized? (external-identity-exists? authn-method external-login instance)
        exist-sanitized? (external-identity-exists? authn-method sanitized-login instance)
        query-identifier (fn [i] (try
                                   (crud/retrieve-by-id (str "user-identifier/" (u/md5 i)))
                                   (catch Exception _
                                     nil)))
        sanitized-record (when exist-sanitized?
                           (query-identifier (generate-identifier authn-method sanitized-login instance)))]
    (when (and (not= external-login sanitized-login)
               exist-sanitized?
               (not exist-unsanitized?))
      (do
        (add-user-identifier! username authn-method external-login instance)
        (db/delete sanitized-record {:user-name "INTERNAL" :user-roles ["ADMIN"]})))))


(defn find-username-by-identifier
  [authn-method instance external-login]
  (let [identifier (generate-identifier authn-method external-login instance)
        sanitized-identifier (generate-identifier authn-method (sanitize-login-name external-login) instance)
        query-identifier (fn [i] (try
                                   (crud/retrieve-by-id (str "user-identifier/" (u/md5 i)))
                                   (catch Exception _
                                     nil)))
        unsanitized-record (query-identifier identifier)
        sanitized-record (query-identifier sanitized-identifier)
        user-identifier-in-use (or unsanitized-record sanitized-record)

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
                                                     external-login ". But found " (count users)))))
        username (cond user-identifier-in-use (some-> user-identifier-in-use
                                                      :user
                                                      :href
                                                      (ignore-user-when-deleted)
                                                      (str/split #"/")
                                                      second)
                       (= (count matched-users-fallback) 1) (get-user matched-users-fallback)
                       (> (count matched-users-fallback) 1) (throw-ex matched-users-fallback))]
    (do
      ;;update mangled identifiers to their unmangled version when needed
      (update-sanitized-identifiers! username authn-method instance external-login)
      username)))


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
