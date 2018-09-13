(ns com.sixsq.slipstream.ssclj.resources.deployment-parameter
  (:require
    [clojure.string :as s]

    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.deployment :as d]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-parameter :as deployment-parameter]
    [com.sixsq.slipstream.util.response :as r]
    [superstring.core :as str]
    [taoensso.timbre :as log]))

(def ^:const resource-name "DeploymentParameter")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "DeploymentCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

;; only authenticated users can view and create credentials
(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})


(defn parameter->uiid
  [deployment-href nodeID name]
  (let [id (s/join ":" [deployment-href nodeID name])]
    (u/from-data-uuid id)))


(def next-state-machine-transition-map {"Executing"      "SendingReports"
                                        "SendingReports" "Ready"
                                        "Ready"          "Ready"
                                        "Done"           "Done"
                                        "Aborted"        "Aborted"
                                        "Cancelled"      "Cancelled"})


(defn next-state
  [current-state]
  (let [next-state (get next-state-machine-transition-map current-state)]
    (if (nil? next-state)
      (throw (r/ex-bad-request (str "complete state invalid: " current-state)))
      next-state)))


(defn is-complete-parameter?
  [name]
  (= name "complete"))


(defn update-state
  [new-state deployment-href]
  (let [uuid (parameter->uiid deployment-href nil "ss:state")
        content-request {:params   {:resource-name resource-url
                                    :uuid          uuid}
                         :identity std-crud/internal-identity
                         :body     {:value new-state}}
        {:keys [status] :as response} (-> content-request crud/edit)]
    (when (not= status 200)
      (log/error response)
      (throw (r/ex-response (str "A failure happened during update of deployment state." response) 500)))))


;;
;; multimethod for ACLs
;;

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))


(def validate-fn (u/create-spec-validation-fn ::deployment-parameter/deployment-parameter))
(defmethod crud/validate resource-uri
  [{:keys [name value deployment] :as resource}]
  (let [deployment-href (:href deployment)]
    (case name
      "complete" (some-> value
                         (next-state)
                         (update-state deployment-href))
      "ss:abort" (when value (update-state "Aborted" deployment-href))
      "ss:state" (let [deployment-request {:params      {:resource-name d/resource-url
                                                         :uuid          (u/document-id deployment-href)}
                                           :cimi-params {:select #{"keepRunning"}}
                                           :identity    std-crud/internal-identity}
                       deployment-data (-> deployment-request crud/retrieve :body)
                       keep-running (get deployment-data :keepRunning "Always")]
                   (when (or (and (= keep-running "Never") (#{"Ready" "Aborted"} value))
                             (and (= keep-running "On Success") (= value "Aborted"))
                             (and (= keep-running "On Error") (= value "Ready")))
                     (crud/do-action {:params   {:action        "stop"
                                                 :resource-name d/resource-url
                                                 :uuid          (u/document-id deployment-href)}
                                      :identity std-crud/internal-identity})))
      nil))
  (validate-fn resource))

;;
;; set the resource identifier to "deployment-parameter/predictable-uuid3-from-string"
;;

(defmethod crud/new-identifier resource-name
  [{:keys [deployment nodeID name] :as parameter} resource-name]
  (->> (parameter->uiid (:href deployment) nodeID name)
       (str resource-url "/")
       (assoc parameter :id)))

;;
;; CRUD operations
;;
(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))
(defmethod crud/add resource-name
  [request]
  (add-impl request))

(def edit-impl (std-crud/edit-fn resource-name))
(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(def retrieve-impl (std-crud/retrieve-fn resource-name))
(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(def delete-impl (std-crud/delete-fn resource-name))
(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
  [request]
  (query-impl request))


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize resource-url ::deployment-parameter/deployment-parameter))
