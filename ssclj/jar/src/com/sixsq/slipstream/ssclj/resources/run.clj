(ns com.sixsq.slipstream.ssclj.resources.run
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.run]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [superstring.core :as str]
    [ring.util.response :as r]
    [com.sixsq.slipstream.util.response :as sr]
    [com.sixsq.slipstream.db.impl :as db]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.sse :as sse]
    [clojure.core.async :as async]
    [zookeeper :as zk]
    )
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-name "Run")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "RunCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})
(def port 2181)

(defn connect
  ([] (connect port))
  ([port]
   (zk/connect (str "127.0.0.1:" port))))

(def client (connect))
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

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [request]
  (add-impl request))

(def node "/hello")

(defn get-data
  "Get data, as string, from the znode"
  [client path]
  (String. (:data (zk/data client path))))

(defn watch-fn [event-ch {:keys [event-type path :as zk-event]}]
  (when (= event-type :NodeDataChanged)
    (let [event {:id   (java.util.UUID/randomUUID)
                 :name "foo"
                 :data (get-data client path)}]
      (async/>!! event-ch event)))
  (zk/data client node :watcher (partial watch-fn event-ch)))

(def handler
  (sse/event-channel-handler
    (fn [request response raise event-ch]
      (zk/data client node :watcher (partial watch-fn event-ch)))
    {:on-client-disconnect #(log/debug "sse/on-client-disconnect: " %)}))

(defn retrieve-fn
  [resource-name]
  (fn [{{uuid :uuid} :params :as request}]
    (try
      (-> (str (u/de-camelcase resource-name) "/" uuid)
          (db/retrieve request)
          (a/can-view? request)
          (crud/set-operations request))
      (catch ExceptionInfo ei
        (ex-data ei)))))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  (handler request))

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
