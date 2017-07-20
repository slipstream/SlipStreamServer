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
    response
    ))

(defmethod crud/add resource-name
  [request]
  (add-impl request))
;
;(def node "/hello")
;
;(defn get-data
;  "Get data, as string, from the znode"
;  [client path]
;  (String. (:data (zk/data client path))))
;
;(defn watch-fn [event-ch {:keys [event-type path :as zk-event]}]
;  (when (= event-type :NodeDataChanged)
;    (let [event {:id   (java.util.UUID/randomUUID)
;                 :name "foo"
;                 :data (get-data client path)}]
;      (async/>!! event-ch event)))
;  (zk/data client node :watcher (partial watch-fn event-ch)))
;
;(def handler
;  (sse/event-channel-handler
;    (fn [request response raise event-ch]
;      (zk/data client node :watcher (partial watch-fn event-ch)))
;    {:on-client-disconnect #(log/debug "sse/on-client-disconnect: " %)}))

(defn retrieve-impl
  [{{uuid :uuid} :params :as request}]
  (try
    (let [run-parameter (-> (str (u/de-camelcase resource-name) "/" uuid)
                            (db/retrieve request)
                            (a/can-view? request)
                            (crud/set-operations request))
          run-id (:run-id run-parameter)
          node-name (:node-name run-parameter)
          node-index (:node-index run-parameter)
          name (:name run-parameter)
          value (:data (uzk/get-data (zkru/parameter-znode-path run-id node-name node-index name))) ;TODO what if data not found
          response (assoc run-parameter :value value)
          ]
      (r/json-response response))
    (catch ExceptionInfo ei
      (ex-data ei))))

(defmethod crud/retrieve resource-name
  [request]
  #_(handler request)
  (retrieve-impl request))

(def edit-impl (std-crud/edit-fn resource-name))

(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))
