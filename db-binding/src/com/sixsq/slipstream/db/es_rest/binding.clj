(ns com.sixsq.slipstream.db.es-rest.binding
  "Binding protocol implemented for an Elasticsearch database that makes use
   of the Elasticsearch REST API."
  (:require
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.util.response :as response]
    [com.sixsq.slipstream.db.binding :refer [Binding]]
    [qbits.spandex :as spandex]
    [clojure.pprint :refer [pprint]])
  (:import
    (java.io Closeable)))


(def ^:const index-name "resources-index")


(defn create-client
  [options]
  (spandex/client options))


(defn add-data
  [client {:keys [id] :as data}]
  (try
    (let [[collection-id uuid] (cu/split-id id)
          response (spandex/request client {:url    [index-name collection-id uuid :_create]
                                            :method :put
                                            :body   data})
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
                                          :body   data})
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
        (get-in response [:body :_source])
        (response/response-not-found id)))
    (catch Exception e
      (let [response (ex-data e)
            status (:status response)]
        (if (= 404 status)
          (response/response-not-found id)
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
        (response/response-not-found id)))
    (catch Exception e
      (let [response (ex-data e)
            status (:status response)]
        (if (= 404 status)
          (response/response-not-found id)
          (response/response-error (str "unexpected error deleting " id)))))))


(defn query-data
  [client collection-id options]
  (let [query {:query {:match_all {}}}
        response (spandex/request client {:url    [index-name collection-id :_search]
                                          :method :post
                                          :body   query})
        success? (-> response :body :_shards :successful pos?)
        count-before-pagination (-> response :body :hits :total)
        aggregations (-> response :body :hits :aggregations)
        meta (cond-> {:count count-before-pagination}
                     aggregations (assoc :aggregations aggregations))
        hits (->> response
                  :body
                  :hits
                  :hits
                  (map :_source))]
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
