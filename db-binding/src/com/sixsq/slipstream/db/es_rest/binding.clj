(ns com.sixsq.slipstream.db.es-rest.binding
  "Binding protocol implemented for an Elasticsearch database that makes use
   of the Elasticsearch REST API."
  (:require
    [qbits.spandex :as spandex]
    [com.sixsq.slipstream.db.binding :refer [Binding]]
    [com.sixsq.slipstream.db.es-rest.acl :as acl]
    [com.sixsq.slipstream.db.es-rest.filter :as filter]
    [com.sixsq.slipstream.db.es-rest.order :as order]
    [com.sixsq.slipstream.db.es-rest.pagination :as paging]
    [com.sixsq.slipstream.db.es-rest.select :as select]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.utils.acl :as acl-utils]
    [com.sixsq.slipstream.util.response :as response])
  (:import
    (java.io Closeable)))


(def ^:const index-name "resources-index")


(defn create-client
  [options]
  (spandex/client options))


(defn prepare-data [data]
  (->> data
       acl-utils/force-admin-role-right-all
       acl-utils/denormalize-acl))


(defn add-data
  [client {:keys [id] :as data}]
  (try
    (let [[collection-id uuid] (cu/split-id id)
          response (spandex/request client {:url    [index-name collection-id uuid :_create]
                                            :method :put
                                            :body   (prepare-data data)})
          success? (pos? (get-in response [:body :_shards :successful]))]
      (if success?
        (response/response-created id)
        (response/response-conflict id)))
    (catch Exception e
      (let [response (ex-data e)]
        (if (= 409 (-> response :body :status))
          (response/response-conflict id)
          (response/response-error (str "unexpected exception: " e)))))))


(defn update-data
  [client {:keys [id] :as data}]
  (let [[collection-id uuid] (cu/split-id id)
        response (spandex/request client {:url    [index-name collection-id uuid]
                                          :method :put
                                          :body   (prepare-data data)})
        success? (pos? (get-in response [:body :_shards :successful]))]
    (if success?
      (response/response-updated id)
      (response/response-conflict id))))


(defn find-data
  [client id]
  (try
    (let [[collection-id uuid] (cu/split-id id)
          response (spandex/request client {:url    [index-name collection-id uuid]
                                            :method :get})
          found? (get-in response [:body :found])]
      (if found?
        (-> response :body :_source acl-utils/normalize-acl)
        (throw (response/ex-not-found id))))
    (catch Exception e
      (let [response (ex-data e)
            status (:status response)]
        (if (= 404 status)
          (throw (response/ex-not-found id))
          (response/response-error (str "unexpected error retrieving " id)))))))


(defn delete-data
  [client id]
  (try
    (let [[collection-id uuid] (cu/split-id id)
          response (spandex/request client {:url    [index-name collection-id uuid]
                                            :method :delete})
          success? (pos? (get-in response [:body :_shards :successful]))
          found? (get-in response [:body :found])]
      (if found?
        (if success?
          (response/response-deleted id)
          (response/response-error (str "could not delete document " id)))
        (throw (response/ex-not-found id))))
    (catch Exception e
      (let [response (ex-data e)
            status (:status response)]
        (if (= 404 status)
          (throw (response/ex-not-found id))
          (response/response-error (str "unexpected error deleting " id)))))))


(defn query-data
  [client collection-id {:keys [cimi-params] :as options}]
  (let [paging (paging/paging cimi-params)
        orderby (order/sorters cimi-params)
        selected (select/select cimi-params)
        query {:query (acl/and-acl (filter/filter cimi-params) options)}
        body (merge paging orderby selected query)
        response (spandex/request client {:url    [index-name collection-id :_search]
                                          :method :post
                                          :body   body})
        success? (-> response :body :_shards :successful pos?)
        count-before-pagination (-> response :body :hits :total)
        aggregations (-> response :body :hits :aggregations)
        meta (cond-> {:count count-before-pagination}
                     aggregations (assoc :aggregations aggregations))
        hits (->> response :body :hits :hits (map :_source) (map acl-utils/normalize-acl))]
    (if success?
      [meta hits]
      (response/response-error "error when querying database"))))


(deftype ElasticsearchRestBinding [client]
  Binding

  (add [_ data options]
    (add-data client data))


  (add [_ collection-id data options]
    (add-data client data))


  (retrieve [_ id options]
    (find-data client id))


  (delete [_ {:keys [id]} options]
    (delete-data client id))


  (edit [_ data options]
    (update-data client data))


  (query [_ collection-id options]
    (query-data client collection-id options))


  Closeable
  (close [_]
    (spandex/close! client)))
