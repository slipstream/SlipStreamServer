;; database implementation of Binding protocol
(ns com.sixsq.slipstream.ssclj.db.database-binding
  (:refer-clojure                                           :exclude [update])
  (:require
    [com.sixsq.slipstream.ssclj.db.binding                  :refer [Binding]]
    [com.sixsq.slipstream.ssclj.db.filesystem-binding-utils :refer [serialize deserialize]]
    [com.sixsq.slipstream.ssclj.database.korma-helper       :as kh]
    [com.sixsq.slipstream.ssclj.database.ddl                :as ddl]
    [com.sixsq.slipstream.ssclj.resources.common.utils      :as u]
    [korma.core                                             :refer :all]
    [ring.util.response                                     :as r]))

(defn init-db
  []  
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

(defn- insert-resource 
  [id data]
  (insert resources (values {:id id :data (serialize data)})))

(defn- update-resource 
  [id data]
  (update resources
    (set-fields {:data data})
    (where {:id id})))

(defn find-resource
  [id]
  (-> (select resources (where {:id id}) (limit 1))
      first
      :data
      deserialize))

(defn dispatch-fn   
  [collection-id options]
  collection-id)

(defmulti  find-resources dispatch-fn)
(defmethod find-resources :default
  [collection-id options]  
  (->>  (select resources (where {:id [like (str collection-id"%")]}))
        (map :data)
        (map deserialize)))

(defn- delete-resource 
  [id]
  (delete resources (where {:id id})))

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
    (check-conflict id)      
    (insert-resource id data)
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
    (update-resource id data))
  
  (query [this collection-id options]
    (find-resources collection-id options)))

(defn get-instance []  
  (init-db)
  (DatabaseBinding. ))
