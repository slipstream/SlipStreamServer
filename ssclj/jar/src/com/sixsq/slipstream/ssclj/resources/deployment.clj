(ns com.sixsq.slipstream.ssclj.resources.deployment
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.deployment-parameter :as dp]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [create-identity-map]]
    [com.sixsq.slipstream.ssclj.resources.deployment.state-machine :as dsm]
    [com.sixsq.slipstream.ssclj.resources.deployment.utils :as du]
    [com.sixsq.slipstream.auth.acl :as a]
    [superstring.core :as str]
    [ring.util.response :as r]
    [com.sixsq.slipstream.util.response :as sr]
    [com.sixsq.slipstream.db.impl :as db]
    [clojure.tools.logging :as log]
    [clojure.core.async :as async]
    [clj-time.core :as time]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [clj-http.client :as http])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-name du/deployment-resource-name)

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url du/deployment-resource-url)

(def ^:const collection-name "DeploymentCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

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
;; validate create requests for subclasses of users
;; different create (registration) requests may take different inputs
;;
(defn dispatch-on-registration-method [resource]
  (get-in resource [:deploymentTemplate :method]))

(defmulti create-validate-subtype dispatch-on-registration-method)

(defmethod create-validate-subtype :default
  [resource]
  (logu/log-and-throw-400 "missing or invalid DeploymentTemplate reference"))

(defmethod crud/validate create-uri
  [resource]
  (create-validate-subtype resource))

;;
;; template processing
;;

(defn dispatch-conversion
  [resource _]
  (:method resource))

(defmulti tpl->deployment dispatch-conversion)

;; default implementation throws if the registration method is unknown
(defmethod tpl->deployment :default
  [resource request]
  (logu/log-and-throw-400 "missing or invalid DeploymentTemplate reference"))

;;
;; CRUD operations
;;

(defmethod crud/new-identifier resource-name
  [json _]
  json)

(defn add-impl [{body :body :as request}]
  (a/can-modify? {:acl collection-acl} request)
  (let [new-deployment (-> body
                           (dissoc :created :updated :resourceURI :operations)
                           (crud/new-identifier resource-name)
                           (assoc :resourceURI resource-uri)
                           u/update-timestamps
                           (crud/add-acl request)
                           (assoc :state dsm/initializing-state))
        response (db/add resource-name (crud/validate new-deployment) {})]
    response))


(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap {:identity (:identity request)}
        desc-attrs (u/select-desc-keys body)
        {:keys [id] :as body} (-> body
                                  (assoc :resourceURI create-uri)
                                  (update-in [:deploymentTemplate] dissoc :method) ;; forces use of template reference
                                  (std-crud/resolve-hrefs idmap)
                                  (update-in [:deploymentTemplate] merge desc-attrs) ;; validate desc attrs
                                  (crud/validate)
                                  (:deploymentTemplate)
                                  (tpl->deployment request)
                                  (merge desc-attrs))]      ;; ensure desc attrs are added
    (add-impl (assoc request :id id :body body))))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))                                    ;TODO cleanup of deployment parameter and in zk should also be done

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))

;;
;; override the operations method to add describe action
;;

(defmethod crud/set-operations resource-uri
  [{:keys [id resourceURI] :as resource} request]
  (let [href-start (str id "/start")
        href-terminate (str id "/terminate")
        start-op {:rel (:start c/action-uri) :href href-start}
        terminate-op {:rel (:terminate c/action-uri) :href href-terminate}]
    (try
      (a/can-modify? resource request)
      (let [ops (if (.endsWith resourceURI "Collection")
                  [{:rel (:add c/action-uri) :href id}]
                  [{:rel (:delete c/action-uri) :href id}
                   start-op
                   terminate-op])]
        (assoc resource :operations ops))
      (catch Exception e
        (if (.endsWith resourceURI "Collection")
          (dissoc resource :operations)
          (assoc resource :operations [start-op terminate-op]))))))

;;
;; actions
;;

(defn add-start-time [deployment]
  (let [now (u/time-now)]
    (assoc deployment :start-time now)))

(defn add-end-time [deployment]
  (let [now (u/time-now)]
    (assoc deployment :end-time now)))

(defmethod crud/do-action [resource-url "start"]
  [{{uuid :uuid} :params identity :identity username :user-name :as request}]
  (try
    (let [current (-> (str (u/de-camelcase resource-name) "/" uuid)
                      (db/retrieve request)
                      (a/can-modify? request))
          deployment (-> current
                         (add-start-time)
                         (u/update-timestamps)
                         (crud/validate))]
      (du/create-parameters identity current)
      (http/post (str du/slipstream-java-endpoint "/run/" uuid) {:headers {"slipstream-authn-info" username}
                                                                 "Accept" "application/json"})
      (db/edit deployment request))
    (catch ExceptionInfo ei
      (ex-data ei))))

(defmethod crud/do-action [resource-url "terminate"]
  [{{uuid :uuid} :params identity :identity username :user-name :as request}]
  (try
    (let [{current-state :state :as current} (-> (str (u/de-camelcase resource-name) "/" uuid)
                                                 (db/retrieve request)
                                                 (a/can-modify? request))
          {id :id new-state :state :as deployment} (-> current
                                                       (add-end-time)
                                                       (update :state (constantly dsm/cancelled-state))
                                                       (u/update-timestamps)
                                                       (crud/validate))]
      (http/delete (str du/slipstream-java-endpoint "/run/" uuid) {:headers {"slipstream-authn-info" username
                                                                             "Accept" "application/json"}})
      (if (dsm/can-terminate? current-state)
        (du/move-deployment-next-state id new-state)
        (du/set-global-deployment-parameter id "state" dsm/cancelled-state)) ;TODO delete znode of deployment, to be done also for done state, fetch runtime-param of old run should not fetch from zookeeper
      (db/edit deployment request))
    (catch ExceptionInfo ei
      (ex-data ei))))