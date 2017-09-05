(ns com.sixsq.slipstream.ssclj.resources.deployment
  (:require
    [clojure.stacktrace :as st]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.deployment-parameter :as dp]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [create-identity-map]]
    [com.sixsq.slipstream.ssclj.resources.zk.deployment.state-machine :as zdsm]
    [com.sixsq.slipstream.auth.acl :as a]
    [superstring.core :as str]
    [ring.util.response :as r]
    [com.sixsq.slipstream.util.response :as sr]
    [com.sixsq.slipstream.db.impl :as db]
    [clojure.tools.logging :as log]
    [clojure.core.async :as async]
    [clj-time.core :as time])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-name "Deployment")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "DeploymentCollection")

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

(def validate-fn (u/create-spec-validation-fn :cimi/deployment))
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
  [json _]
  json)

(defn create-parameter [deployment-parameter]
  (try
    (let [request {:params   {:resource-name dp/resource-url}
                   :identity (create-identity-map ["super" #{"ADMIN"}])
                   :body     deployment-parameter}
          {:keys [status body]} (dp/add-impl request)]
      (case status
        201 (log/info "created deployment-parameter: " body)
        (log/info "unexpected status code when creating deployment-parameter resource:" status)))
    (catch Exception e
      (log/warn "error when creating deployment-parameter resource: " (str e) "\n"
                (with-out-str (st/print-cause-trace e))))))

(defn create-parameters [identity {nodes :nodes deployment-href :id state :state}] ; deployment parameter state should not
  (let [user (:current identity)]
    (create-parameter {:deployment-href deployment-href :name "state" :value state :type "deployment" :acl {:owner {:principal "ADMIN"
                                                                                                                    :type      "ROLE"}
                                                                                                            :rules [{:principal user
                                                                                                                     :type      "USER"
                                                                                                                     :right     "VIEW"}]}})
    (doseq [n nodes]
      ()
      (let [node-name (name (key n))
            multiplicity (read-string (get-in (val n) [:parameters :multiplicity :default-value] "1"))]
        (doseq [i (range 1 (inc multiplicity))]
          (create-parameter {:deployment-href deployment-href :node-name node-name :node-index i :type "node-instance"
                             :name            "vmstate" :value "init" :acl {:owner {:principal "ADMIN"
                                                                                    :type      "ROLE"}
                                                                            :rules [{:principal user
                                                                                     :type      "USER"
                                                                                     :right     "MODIFY"}]}}))))))

(defn add-impl [{body :body :as request}]
  (a/can-modify? {:acl collection-acl} request)
  (let [new-deployment (-> body
                           (dissoc :created :updated :resourceURI :operations)
                           (crud/new-identifier resource-name)
                           (assoc :resourceURI resource-uri)
                           u/update-timestamps
                           (crud/add-acl request)
                           (assoc :state zdsm/initial-state))
        response (db/add resource-name (crud/validate new-deployment) {})]
    response))

(defmethod crud/add resource-name
  [request]
  (add-impl request))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
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

;;
;; override the operations method to add describe action
;;

(defmethod crud/set-operations resource-uri
  [{:keys [id resourceURI] :as resource} request]
  (let [href (str id "/start")]
    (try
      (a/can-modify? resource request)
      (let [ops (if (.endsWith resourceURI "Collection")
                  [{:rel (:add c/action-uri) :href id}]
                  [{:rel (:edit c/action-uri) :href id}
                   {:rel (:delete c/action-uri) :href id}
                   {:rel (:start c/action-uri) :href href}])]
        (assoc resource :operations ops))
      (catch Exception e
        (if (.endsWith resourceURI "Collection")
          (dissoc resource :operations)
          (assoc resource :operations [{:rel (:start c/action-uri) :href href}]))))))

;;
;; actions
;;

(defn add-start-time [deployment]
  (let [now (u/time-now)]
    (assoc deployment :start-time now)))

(defmethod crud/do-action [resource-url "start"]
  [{{uuid :uuid} :params identity :identity :as request}]
  (try
    (let [current (-> (str (u/de-camelcase resource-name) "/" uuid)
                      (db/retrieve request)
                      (a/can-modify? request))
          deployment (-> current
                         (add-start-time)
                         (u/update-timestamps)
                         (crud/validate)
                         (db/edit request))]
      (create-parameters identity current)
      deployment)
    (catch ExceptionInfo ei
      (ex-data ei))))