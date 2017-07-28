(ns com.sixsq.slipstream.ssclj.resources.run-parameter
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.run-parameter]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [superstring.core :as str]
    [com.sixsq.slipstream.util.response :as r]
    [com.sixsq.slipstream.db.impl :as db]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.ssclj.util.sse :as sse]
    [clojure.core.async :as async]
    [zookeeper :as zk]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [com.sixsq.slipstream.ssclj.resources.zk.run.utils :as zkru]
    )
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-name "RunParameter")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "RunParameterCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn :cimi/run-parameter))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; CRUD operations
;;

(defn add-impl [{:keys [body] :as request}]
  (a/can-modify? {:acl collection-acl} request)
  (let [run-parameter (-> body
                          u/strip-service-attrs
                          (dissoc :value)
                          (crud/new-identifier resource-name)
                          (assoc :resourceURI resource-uri)
                          u/update-timestamps
                          (crud/add-acl request)
                          crud/validate)
        response (db/add resource-name run-parameter {})
        run-id (:run-id body)
        node-name (:node-name body)
        node-index (:node-index body)
        name (:name body)
        value (:value body)
        node-path (zkru/parameter-znode-path run-id node-name node-index name)]
    (uzk/create-all node-path :persistent? true)
    (uzk/set-data node-path value)
    response))

(defn transiant-watch-fn [event-ch id name {:keys [event-type path :as zk-event]}]
  (when (= event-type :NodeDataChanged)
    (let [data (uzk/get-data path)]
      (sse/send-event id name data event-ch)
      (async/close! event-ch))))

(defn persistent-watch-fn [event-ch id name {:keys [event-type path :as zk-event]}]
  (when (= event-type :NodeDataChanged)
    (let [data (uzk/get-data path :watcher (partial persistent-watch-fn event-ch id name))]
      (sse/send-event id name data event-ch))))

(defn send-event-and-set-watcher
  [event-ch watch-fn {id :id name :name :as run-parameter}]
  (let [node-path (zkru/run-parameter-znode-path run-parameter)
        value (uzk/get-data node-path :watcher (partial watch-fn event-ch id name))]
    (sse/send-event id name value event-ch)))

(defn retrieve-run-parameter [{{uuid :uuid} :params :as request}]
  (try
    (let [run-parameter (-> (str (u/de-camelcase resource-name) "/" uuid)
                            (db/retrieve request)
                            (a/can-view? request)
                            (crud/set-operations request))
          value (uzk/get-data (zkru/run-parameter-znode-path run-parameter))] ;TODO what if data not found
      (assoc run-parameter :value value))
    (catch ExceptionInfo ei
      (ex-data ei))))

(def retrieve-sse-impl
  (sse/event-channel-handler
    (fn [request response raise event-ch]
      (let [{id :id name :name :as run-parameter} (retrieve-run-parameter request)
            node-path (zkru/run-parameter-znode-path run-parameter)
            value (uzk/get-data node-path)]
        (send-event-and-set-watcher event-ch transiant-watch-fn run-parameter)))
    {:on-client-disconnect #(log/debug "sse/on-client-disconnect: " %)}))

(defn retrieve-json-impl [request]
  (r/json-response (retrieve-run-parameter request)))

(defmethod crud/retrieve resource-name
  [{{accept :accept} :headers :as request}]
  (case accept
    "text/event-stream" retrieve-sse-impl
    retrieve-json-impl))

(defn edit-impl
  [{{uuid :uuid} :params body :body :as request}]
  (let [current (-> (str (u/de-camelcase resource-name) "/" uuid)
                    (db/retrieve request)
                    (a/can-modify? request))
        merged (merge current body)
        value (:value merged)]
    (-> merged
        (u/update-timestamps)
        (crud/validate)
        (dissoc :value)
        (db/edit request))
    (when value
      (let [name (:name merged)]
        (if (contains? #{"state"} name)
          (logu/log-and-throw-400 (str "run parameter " name " cannot be set"))
          (uzk/set-data (zkru/run-parameter-znode-path merged) value)))))) ;TODO what if znode not found

(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-sse-impl
  (sse/event-channel-handler
    (fn [request response raise event-ch]
      (a/can-view? {:acl collection-acl} request)
      (let [options (select-keys request [:identity :query-params :cimi-params :user-name :user-roles])
            [count-before-pagination entries] (db/query resource-name options)]
        (doall (map (partial send-event-and-set-watcher event-ch persistent-watch-fn) entries))))
    {:on-client-disconnect #(log/debug "sse/on-client-disconnect: " %)}))

(def query-json-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [{{accept :accept} :headers :as request}]
  (case accept
    "text/event-stream" query-sse-impl
    query-json-impl))
