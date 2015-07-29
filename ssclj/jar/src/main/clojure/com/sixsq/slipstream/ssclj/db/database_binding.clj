;; database implementation of Binding protocol
(ns com.sixsq.slipstream.ssclj.db.database-binding
  (:refer-clojure :exclude [update])
  (:require
    [ring.util.response                                     :as r]

    [clojure.java.jdbc                                      :refer :all :as jdbc]
    [korma.core                                             :refer :all]
    [honeysql.helpers                                       :as hh]
    [honeysql.core                                          :as hq]
    [com.sixsq.slipstream.ssclj.database.korma-helper       :as kh]
    [com.sixsq.slipstream.ssclj.api.acl                     :as acl]

    [com.sixsq.slipstream.ssclj.db.binding                  :refer [Binding]]
    [com.sixsq.slipstream.ssclj.database.ddl                :as ddl]
    [com.sixsq.slipstream.ssclj.resources.common.utils      :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(defn init-db
  []
  (acl/-init)
  (kh/korma-init)
  (ddl/create-table! "resources" (ddl/columns "id" "VARCHAR(100)" "data" "VARCHAR(10000)"))
  (defentity resources))

;;
;; Korma SQL primitives
;;

(defn exist-in-db?
  [id]
  (not (empty? (select resources (where {:id id}) (limit 1)))))

(defn- check-conflict
  [id]
  (when (exist-in-db? id)
    (throw (u/ex-conflict id))))

(defn- check-exist
  [id]
  (when-not (exist-in-db? id)
    (throw (u/ex-not-found id))))

(defn- insert-acl
  [id data]
  (when (:acl data)
    (acl/insert-resource id
                         (u/resource-name (:id data))
                         (acl/types-principals-from-acl (:acl data)))))

(defn- insert-resource
  [id data]
  (insert resources (values {:id id :data (u/serialize data)}))
  (insert-acl id data))

(defn- update-resource
  [id data]
  (update resources
    (set-fields {:data (u/serialize data)})
    (where {:id id})))

(defn id-matches?
  [id]
  (if id
    [:and [:= :a.principal-type "USER"] [:= :a.principal-name id]]
    [:= 0 1]))

(defn roles-in?
  [roles]
  (cond
    (some #{"ADMIN"} roles) [:= 1 1]
    (seq roles)             [:and [:= :a.principal-type "ROLE"]
                                  [:in :a.principal-name roles]]
    :else                   [:= 0 1]))

(defn neither-id-roles?
  [id roles]
  (not (or id (seq roles))))

(defn id-roles
  [options]
  (-> options      
      :identity
      :authentications
      (get (get-in options [:identity :current]))
      ((juxt :identity :roles))))

(defn sql
  [collection-id id roles]
  (->   (hh/select :r.*)
        (hh/from [:acl :a] [:resources :r])
        (hh/where [:and
                    [:like :r.id (str (u/de-camelcase collection-id) "%")]
                    [:= :r.id :a.resource-id] ;; join acl with resources
                    [:or
                      (id-matches? id)        ;; an acl line with given id
                      (roles-in? roles)]])    ;; an acl line with one of the given roles
        (hh/modifiers :distinct)
        (hq/format :quoting :ansi)))

(defn find-resource
  [id]
  (-> (select resources (where {:id id}) (limit 1))
      first
      :data
      u/deserialize))

(defn dispatch-fn
  [collection-id options]
  collection-id)

(defn store-dispatch-fn
  [collection-id id data]
  collection-id)

(defmulti store-in-db store-dispatch-fn)
(defmethod store-in-db :default
  [collection-id id data]
  (check-conflict id)
  (insert-resource id data))

(defmulti  find-resources dispatch-fn)
(defmethod find-resources :default
 [collection-id options]  
 (let [ [id roles]      (id-roles options)]
   (if (neither-id-roles? id roles)
     []
     (->> (sql collection-id id roles)
          (jdbc/query kh/db-spec)                 
          (map :data)
          (map u/deserialize)))))


(defn- delete-resource
  [id]
  (delete resources (where {:id id})))

(defn- response-updated
  [id]
  (-> (str "updated " id)
      (u/map-response 200 id)))

(defn- response-created
  [id]
  (-> (str "created " id)
      (u/map-response 201 id)
      (r/header "Location" id)))

(defn- response-deleted
  [id]
  (-> (str id " deleted")
      (u/map-response 204 id)))

(deftype DatabaseBinding []
  Binding
  (add [this collection-id {:keys [id] :as data}]
    (store-in-db collection-id id data)
    (response-created id))

  (retrieve [this id]
    (check-exist  id)
    (find-resource id))

  (delete [this {:keys [id]}]
    (check-exist id)
    (delete-resource id)
    (response-deleted id))

  (edit [this {:keys [id] :as data}]
    (check-exist id)
    (update-resource id data)
    (response-updated id))

  (query [this collection-id options]
    (find-resources collection-id options)))

(defn get-instance []
    (init-db)
    (DatabaseBinding. ))