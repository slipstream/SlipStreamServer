(ns com.sixsq.slipstream.auth.utils.db
  (:require
    [com.sixsq.slipstream.db.filter.parser :as parser]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.user-params-template-exec :as up-tmpl-exec])
  (:import
    (java.util UUID)))

;; Only ACTIVE users can log in.  All other states (NEW, SUSPENDED, and DELETED) are disallowed
(def ^:private active-user-filter "(state='ACTIVE')")

(def resource-name "user")

(defn resource-uri
  [name]
  (str resource-name "/" name))

(defn get-all-users
  []
  (try
    (second (db/query resource-name {:user-roles ["ADMIN"]}))
    (catch Exception _ [])))

(defn get-all-user-params
  []
  (try
    (second (db/query "user-param" {:user-roles ["ADMIN"]}))
    (catch Exception _ [])))

(defn get-active-users
  []
  (let [filter {:filter (parser/parse-cimi-filter active-user-filter)}]
    (try
      (second (db/query resource-name {:cimi-params filter
                                       :user-roles  ["ADMIN"]}))
      (catch Exception _ []))))

(defn get-user
  [username]
  (try
    (db/retrieve (resource-uri username) {})
    (catch Exception _ {})))

(defn find-usernames-by-email
  [email]
  (when email
    (let [filter-str (format "emailAddress='%s' and %s" email active-user-filter)
          filter {:filter (parser/parse-cimi-filter filter-str)}
          matched-users (try (second (db/query resource-name {:cimi-params filter
                                                              :user-roles  ["ADMIN"]}))
                             (catch Exception _ []))]
      (set (map :username matched-users)))))

(defn- to-am-kw
  [authn-method]
  (keyword (str (name authn-method) "login")))


(defn find-username-by-authn
  [authn-method authn-id]
  ;;assume authn-method is :github or :oidc (i.e not :githublogin)
  (when (and authn-method authn-id)
    (let [filter-str (format "externalIdentity='%s:%s' and %s" (name authn-method) authn-id active-user-filter)
          filter-str-fallback (format "%s='%s' and %s" (name (to-am-kw authn-method)) authn-id active-user-filter)
          create-filter (fn [filter-string] {:filter (parser/parse-cimi-filter filter-string)})
          filter (create-filter filter-str)
          filter-fallback (create-filter filter-str-fallback)
          query-users (fn [f] (try
                                (second (db/query resource-name {:cimi-params f
                                                                 :user-roles  ["ADMIN"]}))
                                (catch Exception _ [])))
          matched-users (query-users filter)
          matched-users-fallback (query-users filter-fallback)
          get-user (fn [users] (:username (first users)))
          throw-ex (fn [users]  (throw (Exception. (str "There should be only one result for "
                                                       authn-id ". Was " users))) )]
      (cond
        (= (count matched-users) 1) (get-user matched-users)
        (> (count matched-users) 1) (throw-ex matched-users)
        (= (count matched-users-fallback) 1) (get-user matched-users-fallback)
        (> (count matched-users-fallback) 1) (throw-ex matched-users-fallback)))))

(defn get-active-user-by-name
  [username]
  (when username
    (let [filter-str (format "username='%s' and %s" username active-user-filter)
          filter {:filter (parser/parse-cimi-filter filter-str)}]
      (try (-> (db/query "user" {:cimi-params filter
                                 :user-roles  ["ADMIN"]})
               second
               first)
           (catch Exception _ {})))))

(defn user-exists?
  "Verifies that a user with the given username exists in the database no
   matter what the user state is."
  [username]
  (-> username
      get-user
      :state
      nil?
      not))

(defn- to-resource-id
  [n]
  (format "%s/%s" resource-name n))

(defn update-user-authn-info
  [authn-method slipstream-username authn-id]
  (let [body {:id                     (to-resource-id slipstream-username)
              :username               slipstream-username
              (to-am-kw authn-method) authn-id}
        request {:identity       {:current slipstream-username
                                  :authentications
                                           {slipstream-username {:roles #{"USER"} :identity slipstream-username}}}
                 :sixsq.slipstream.authn/claims
                                 {:username slipstream-username :roles "USER"}
                 :params         {:resource-name resource-name}
                 :route-params   {:resource-name resource-name}
                 :user-roles     #{"ANON"}
                 :uri            (format "/api/%s/%s" resource-name slipstream-username)
                 :request-method "PUT"
                 :body           body}]
    (crud/edit request))
  slipstream-username)

(defn- inc-string
  [s]
  (if (empty? s)
    1
    (inc (read-string s))))

(defn- append-number-or-inc
  [name]
  (if-let [tokens (re-find #"(\w*)(_)(\d*)" name)]
    (str (nth tokens 1) "_" (inc-string (nth tokens 3)))
    (str name "_1")))

(defn name-no-collision
  [name existing-names]
  (if ((set existing-names) name)
    (recur (append-number-or-inc name) existing-names)
    name))

(defn existing-user-names
  []
  (let [users (second (db/query "user" {:user-roles ["ADMIN"]}))]
    (map :username users)))

(defn random-password
  []
  (str (UUID/randomUUID)))

(defn user-create-request
  [{:keys [authn-login email authn-method firstname lastname roles organization
           state external-login]}]
  (let [slipstream-username (name-no-collision authn-login (existing-user-names))
        user-resource (cond-> {:href         "user-template/direct"
                               :username     slipstream-username
                               :emailAddress email
                               :password     (random-password)
                               :deleted      false
                               :isSuperUser  false
                               :state        (or state "ACTIVE")}
                              authn-method (assoc :externalIdentity [ (str authn-method ":" (or external-login authn-login))])
                              firstname (assoc :firstName firstname)
                              lastname (assoc :lastName lastname)
                              roles (assoc :roles roles)
                              organization (assoc :organization organization))]
    {:identity     {:current "internal"
                    :authentications
                             {"internal" {:roles #{"ADMIN"}, :identity "internal"}}}
     :sixsq.slipstream.authn/claims
                   {:username "internal", :roles "ADMIN"}
     :params       {:resource-name "user"}
     :route-params {:resource-name "user"}
     :user-roles   #{"ANON"}
     :body         {:userTemplate user-resource}}))

(defn user-param-create-request
  [user-name]
  {:identity     {:current user-name
                  :authentications
                           {user-name {:roles #{"USER"}, :identity user-name}}}
   :sixsq.slipstream.authn/claims
                 {:username user-name, :roles "USER"}
   :params       {:resource-name "user-param"}
   :route-params {:resource-name "user-param"}
   :user-roles   #{"USER"}
   :body         {:userParamTemplate up-tmpl-exec/resource}})

(defn create-user-params!
  [user-name]
  (crud/add (user-param-create-request user-name)))

(defn create-user!
  "Create a new user in the database. Values for 'email' and 'authn-login'
   must be provided. NOTE: The 'authn-login' value may be modified to avoid
   collisions with existing users. The value used to create the account is
   returned."
  ([{:keys [authn-login fail-on-existing?] :as user-record}]
   (let [slipstream-username (name-no-collision authn-login (existing-user-names))]
     (when (or (not fail-on-existing?) (= authn-login slipstream-username))
       (crud/add (user-create-request user-record))
       slipstream-username)))
  ([authn-method authn-login email external-login]
   (create-user! {:authn-login    authn-login               ;; possibly a UUID
                  :authn-method   authn-method
                  :email          email
                  :external-login external-login}))
  ([authn-method authn-login email]
   (create-user! {:authn-login    authn-login
                  :authn-method   authn-method
                  :email          email
                  :external-login authn-login}))            ;;legacy behaviour where external-login=authn-login
  ([authn-login email]
   (create-user! {:authn-login authn-login
                  :email       email})))

(defn find-password-for-username
  [username]
  (:password (get-active-user-by-name username)))

(defn find-roles-for-username
  [username]
  (let [{super? :isSuperUser} (get-active-user-by-name username)]
    (if super? "ADMIN USER ANON" "USER ANON")))
