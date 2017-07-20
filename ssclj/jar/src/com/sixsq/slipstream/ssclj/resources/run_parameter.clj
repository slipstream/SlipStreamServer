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
    [ring.util.response :as r]
    [com.sixsq.slipstream.util.response :as sr]
    [com.sixsq.slipstream.db.impl :as db]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.sse :as sse]
    [clojure.core.async :as async]
    [zookeeper :as zk]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
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
  (let [rp (-> body
               u/strip-service-attrs
               (crud/new-identifier resource-name)
               (assoc :resourceURI resource-uri)
               u/update-timestamps
               (crud/add-acl request)
               crud/validate)
        response (db/add resource-name rp {})
        run-id (:run-id body)
        node-name (:node-name body)
        node-index (:node-index body)
        name (:name body)
        value (:value body)
        node-path (cond
                    (and run-id node-name node-index) (str "/runs/" run-id "/" node-name "/" node-index "/" name)
                    (and run-id node-name) (str "/runs/" run-id "/" node-name "/" name)
                    (and run-id) (str "/runs/" run-id "/" name))
        ]
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
  #_(handler request)
  (retrieve-impl request)
  )

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
