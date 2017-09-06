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
  (let [new-id (str resource-url "/" (du/deployment-parameter-id json))]
    (assoc json :id new-id)))

(defn add-value-deployment-parameter [deployment-parameter & {:keys [watcher]}]
  (try
    (let [value (if watcher
                  (uzk/get-data (zdu/deployment-parameter-znode-path deployment-parameter) :watcher watcher)
                  (uzk/get-data (zdu/deployment-parameter-znode-path deployment-parameter)))]
      (assoc deployment-parameter :value value))
    (catch ExceptionInfo ei                                 ;TODO what if data not found
      (ex-data ei))))

(defn transiant-watch-fn [event-ch id name {:keys [event-type path :as zk-event]}]
  (when (= event-type :NodeDataChanged)
    (let [deployment-parameter (-> (db/retrieve id {})
                                   (add-value-deployment-parameter))]
      (sse/send-event id name deployment-parameter event-ch)
      (async/close! event-ch))))

(defn persistent-watch-fn [event-ch id name {:keys [event-type path :as zk-event]}]
  (when (= event-type :NodeDataChanged)
    (let [deployment-parameter (-> (db/retrieve id {})
                                   (add-value-deployment-parameter :watcher (partial persistent-watch-fn event-ch id name)))]
      (sse/send-event id name deployment-parameter event-ch))))

(defn send-event-and-set-watcher
  [event-ch watch-fn {id :id name :name :as deployment-parameter}]
  (let [deployment-parameter (-> deployment-parameter
                                 (add-value-deployment-parameter :watcher (partial watch-fn event-ch id name)))]
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
            node-path (zdu/deployment-parameter-znode-path deployment-parameter)]
        (send-event-and-set-watcher event-ch transiant-watch-fn deployment-parameter)))
    {:on-client-disconnect #(log/debug "sse/on-client-disconnect: " %)}))

(def retrieve-json-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [{{accept :accept} :headers :as request}]
  (case accept
    "text/event-stream" retrieve-sse-impl
    retrieve-json-impl))

(defn edit-impl
  [{{uuid :uuid} :params body :body :as request}]
  (let [current (-> (str (u/de-camelcase resource-name) "/" uuid)
                    (db/retrieve request)
                    (a/can-modify? request)) ;TODO we should not allow user to update type and some other properties of deployment parameter (use dissoc)
        merged (merge current body)
        value (:value merged)
        deployment-href (:deployment-href merged)]
    (-> merged
        (u/update-timestamps)
        (crud/validate)
        (db/edit request))
    (when value
      ;TODO what if znode not found
      (condp = (:type merged)
        "deployment" (do
                       (uzk/set-data (zdu/deployment-parameter-znode-path merged) value)
                       (du/update-deployment-attribut
                           (du/deployment-href-to-uuid deployment-href) (:name merged) value))
        "node-instance" (condp = (:name merged)
                          "state-complete" (do
                                             (uzk/set-data (zdu/deployment-parameter-znode-path merged) value)
                                             (uzk/delete (zdu/deployment-parameter-node-instance-complete-state-znode-path merged))
                                             (let [children-in-state-count (count (uzk/children (zdu/deployment-parameter-znode-path {:deployment-href deployment-href :name "state"})))]
                                               (when (= 0 children-in-state-count)
                                                 ;TODO function trigger next state for current deployment
                                                 (println "post with next state on run parameter state with next state value")
                                                 (println "create all nodes instances state-compete of next global state")
                                                 )))
                          (uzk/set-data (zdu/deployment-parameter-znode-path merged) value)
                          )))
    merged))

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
    {:on-client-disconnect #(log/debug "sse/on-client-disconnect: " %) :heartbeat-delay 10}))

(def query-json-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [{{accept :accept} :headers :as request}]
  (case accept
    "text/event-stream" query-sse-impl
    query-json-impl))
