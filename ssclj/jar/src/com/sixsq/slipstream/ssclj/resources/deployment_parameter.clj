(ns com.sixsq.slipstream.ssclj.resources.deployment-parameter
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-parameter]
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
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [com.sixsq.slipstream.ssclj.resources.zk.deployment.utils :as zdu]
    [com.sixsq.slipstream.ssclj.resources.deployment.utils :as du]
    )
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-name du/deployment-parameter-resource-name)

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url du/deployment-parameter-resource-url)

(def ^:const collection-name "DeploymentParameterCollection")

(def ^:const resource-uri du/deployment-parameter-resource-uri)

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl du/deployment-parameter-collection-acl)

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn :cimi/deployment-parameter))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; CRUD operations
;;

(defmethod crud/new-identifier resource-name
  [json resource-name]
  (let [new-id (du/deployment-parameter-href json)]
    (assoc json :id new-id)))

(defn deployment-parameter-watch-fn [is-transiant]
  (fn [event-ch id name {:keys [event-type path :as zk-event]}]
    (when (= event-type :NodeDataChanged)
      (let [deployment-parameter (db/retrieve id {})
            value (if is-transiant
                    (zdu/get-deployment-parameter-value deployment-parameter)
                    (zdu/get-deployment-parameter-value
                      deployment-parameter :watcher
                      (partial (deployment-parameter-watch-fn is-transiant) event-ch id name)))
            deployment-parameter (assoc deployment-parameter :value value)]
        (sse/send-event id name deployment-parameter event-ch)
        (when is-transiant (async/close! event-ch))))))

(defn send-event-and-set-watcher
  [event-ch watch-fn {id :id name :name :as deployment-parameter}]
  (let [value (zdu/get-deployment-parameter-value deployment-parameter :watcher (partial watch-fn event-ch id name))
        deployment-parameter (assoc deployment-parameter :value value)]
    (sse/send-event id name deployment-parameter event-ch)))

(defn retrieve-deployment-parameter
  [{{uuid :uuid} :params :as request}]
  (try
    (-> (str (u/de-camelcase resource-name) "/" uuid)
        (db/retrieve request)
        (a/can-view? request)
        (crud/set-operations request))
    (catch ExceptionInfo ei
      (ex-data ei))))

(def retrieve-sse-impl
  (sse/event-channel-handler
    (fn [request response raise event-ch]
      (let [{id :id name :name :as deployment-parameter} (retrieve-deployment-parameter request)
            node-path (zdu/deployment-parameter-path deployment-parameter)]
        (send-event-and-set-watcher event-ch (deployment-parameter-watch-fn true) deployment-parameter)))
    {:on-client-disconnect #(log/debug "sse/on-client-disconnect: " %)}))

(def retrieve-json-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [{{accept :accept} :headers :as request}]
  (case accept
    "text/event-stream" retrieve-sse-impl
    retrieve-json-impl))

(defmethod crud/edit resource-name
  [request]
  (du/edit-deployment-parameter-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-sse-impl
  (sse/event-channel-handler
    (fn [request response raise event-ch]
      (a/can-view? {:acl collection-acl} request)
      (let [options (select-keys request [:identity :query-params :cimi-params :user-name :user-roles])
            [count-before-pagination entries] (db/query resource-name options)
            transient-deployment-parameter-watch (deployment-parameter-watch-fn false)
            send-event-and-set-watcher-partial (partial send-event-and-set-watcher
                                                        event-ch transient-deployment-parameter-watch)]
        (doall (map send-event-and-set-watcher-partial entries))))
    {:on-client-disconnect #(log/debug "sse/on-client-disconnect: " %) :heartbeat-delay 10}))

(def query-json-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [{{accept :accept} :headers :as request}]
  (case accept
    "text/event-stream" query-sse-impl
    query-json-impl))
