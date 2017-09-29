(ns com.sixsq.slipstream.ssclj.resources.deployment.utils
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [clojure.tools.logging :as log]
    [clojure.stacktrace :as st]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [create-identity-map]]
    [com.sixsq.slipstream.ssclj.resources.zk.deployment.utils :as zdu]
    [com.sixsq.slipstream.ssclj.resources.deployment.state-machine :as dsm]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [clojure.string :as string]
    [environ.core :as env])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const deployment-resource-name "Deployment")

(def ^:const deployment-resource-url (u/de-camelcase deployment-resource-name))

(def ^:const deployment-parameter-resource-name "DeploymentParameter")

(def ^:const deployment-parameter-resource-url (u/de-camelcase deployment-parameter-resource-name))

(def ^:const deployment-parameter-resource-uri (str c/slipstream-schema-uri deployment-parameter-resource-name))

(def deployment-parameter-collection-acl {:owner {:principal "ADMIN"
                                                  :type      "ROLE"}
                                          :rules [{:principal "USER"
                                                   :type      "ROLE"
                                                   :right     "MODIFY"}]})

(def slipstream-java-endpoint (or (env/env :slipstream-java-endpoint) "http://localhost:8182"))

(def deployment-parameter-id-separator "_")

(defn deployment-href-to-uuid [href] (string/replace-first href #"^deployment/" ""))

(defn deployment-parameter-href
  [{{deployment-href :href} :deployment node-name :node-name node-index
                            :node-index name :name :as deployment-parameter}]
  (let [deployment-uuid (deployment-href-to-uuid deployment-href)
        deployment-parameter-element (cond
                                       (and deployment-uuid node-name node-index) [deployment-uuid node-name
                                                                                   node-index name]
                                       (and deployment-uuid node-name) [deployment-uuid node-name name]
                                       deployment-uuid [deployment-uuid name])
        deployment-parameter-path (string/join deployment-parameter-id-separator deployment-parameter-element)]
    (string/join "/" [deployment-parameter-resource-url deployment-parameter-path])))

(defn set-global-deployment-parameter
  [deployment-href parameter-name value]
  (try
    (let [deployment-parameter {:deployment {:href deployment-href}
                                :name       parameter-name
                                :value      value}
          current (db/retrieve (deployment-parameter-href deployment-parameter) {})
          merged (-> current
                     (merge deployment-parameter)
                     (u/update-timestamps)
                     (crud/validate))]
      (uzk/set-data (zdu/deployment-parameter-path merged) value)
      (-> merged
          (db/edit {})
          :body))
    (catch ExceptionInfo ei
      (ex-data ei))))

(defn set-deployment-attribute
  [deployment-href attribute-name value]
  (try
    (let [current-deployment (db/retrieve deployment-href {})
          merged-deployment (-> current-deployment
                                (merge {(keyword attribute-name) value})
                                (u/update-timestamps)
                                (crud/validate))]
      (set-global-deployment-parameter deployment-href attribute-name value)
      (-> merged-deployment
          (db/edit {})
          :body))
    (catch ExceptionInfo ei
      (ex-data ei))))

(defn move-deployment-next-state [deployment-href current-state]
  (zdu/check-deployment-lock-and-throw! deployment-href)
  (zdu/lock-deployment-path deployment-href)
  (let [next-state (dsm/get-next-state current-state)       ;TODO final state what should be done?
        deployment (set-deployment-attribute deployment-href "state" next-state)]
    (doseq [n (:nodes deployment)]
      (let [node-name (name (key n))
            multiplicity (read-string (get-in (val n) [:parameters :multiplicity :value] "1"))]
        (doseq [i (range 1 (inc multiplicity))]
          (uzk/create-all
            (zdu/deployment-parameter-node-instance-state-path
              deployment-href node-name i "complete") :persistent? true)))))
  (zdu/unlock-deployment deployment-href))

(defn cancel-deployment [deployment-href]
  (zdu/check-deployment-lock-and-throw! deployment-href)
  (zdu/lock-deployment-path deployment-href)
  (set-global-deployment-parameter deployment-href "state" dsm/cancelled-state)
  ()
  (let [deployment (set-deployment-attribute deployment-href "state" dsm/cancelled-state)]
    (zdu/unlock-deployment deployment-href)
    deployment
    ))

(defn abort-deployment [deployment-href current-state]
  (zdu/check-deployment-lock-and-throw! deployment-href)
  (zdu/lock-deployment-path deployment-href)
  (set-deployment-attribute deployment-href "abort" current-state)
  (set-deployment-attribute deployment-href "state" dsm/aborted-state)
  (zdu/unlock-deployment deployment-href))

(defn create-deployment-parameter [deployment-parameter]
  (let [deployment-parameter (-> deployment-parameter
                                 u/strip-service-attrs
                                 (crud/new-identifier deployment-parameter-resource-name)
                                 (assoc :resourceURI deployment-parameter-resource-uri)
                                 u/update-timestamps
                                 (crud/add-acl {})
                                 crud/validate)
        value (:value deployment-parameter)
        node-path (zdu/deployment-parameter-path deployment-parameter)]
    (uzk/create-all node-path :persistent? true)
    (uzk/set-data node-path value)
    (when (and (= "complete" (:name deployment-parameter))
               (= "node-instance" (:type deployment-parameter)))
      (uzk/create-all
        (zdu/deployment-parameter-node-instance-state-path deployment-parameter) :persistent? true))
    (-> (db/add deployment-parameter-resource-name deployment-parameter {})
        :body)))

(defn edit-deployment-parameter-impl
  [{{uuid :uuid} :params body :body :as request}]
  (let [current (-> (str deployment-parameter-resource-url "/" uuid)
                    (db/retrieve request)
                    (a/can-modify? request))
        merged (->> (dissoc body :type :deployment :node-name :node-index)
                    (merge current))
        value (:value merged)
        parameter-name (:name merged)
        deployment-href (get-in merged [:deployment :href])
        deployment-parameter (-> merged
                                 (u/update-timestamps)
                                 (crud/validate))]
    (when value
      (case (:type deployment-parameter)
        "deployment" (set-deployment-attribute deployment-href parameter-name value)
        "node-instance" (case parameter-name
                          "complete" (do
                                       (zdu/check-deployment-lock-and-throw! deployment-href)
                                       (zdu/check-same-state-and-throw! deployment-href value)
                                       (zdu/complete-node-instance-state merged)
                                       (when (zdu/all-nodes-completed-current-state? deployment-href)
                                         (move-deployment-next-state deployment-href value)))
                          "abort" (do
                                    (zdu/check-deployment-lock-and-throw! deployment-href)
                                    (uzk/set-data (zdu/deployment-parameter-path deployment-parameter) value)
                                    (abort-deployment deployment-href value))
                          (uzk/set-data (zdu/deployment-parameter-path deployment-parameter) value))))
    (db/edit deployment-parameter request)))

(defn create-parameters [identity {nodes :nodes deployment-href :id state :state category :category}]
  (let [user (:current identity)]
    (doseq [[gp-key gp-body] {:category      {:description "Module category" :value category}
                              :complete      {:description "Global complete flag, set when run completed" :value ""}
                              :abort         {:description "Run abort flag, set when aborting" :value ""}
                              :state         {:description "Global execution state" :value state}
                              :url-service   {:description "Optional service URL for the deployment" :value ""}
                              :tags          {:description "Tags (comma separated) or annotations for this VM" :value ""}
                              :recovery-mode {:description "Run abort flag, set when aborting" :value "false"}}]
      (create-deployment-parameter
        {:deployment {:href deployment-href} :name (name gp-key) :value (:value gp-body) :type "deployment"
         :acl        {:owner {:principal "ADMIN"
                              :type      "ROLE"}
                      :rules [{:principal user
                               :type      "USER"
                               :right     "VIEW"}]}}))
    (doseq [[node-key node-params] nodes]
      (let [multiplicity (read-string (get-in node-params [:parameters :multiplicity :value] "1"))]
        (doseq [i (range 1 (inc multiplicity))]
          (doseq [[rp-key rp-body] (:runtime-parameters node-params)]
            (create-deployment-parameter
              {:deployment  {:href deployment-href} :node-name (name node-key) :node-index i :type "node-instance"
               :name        (clojure.string/replace (name rp-key) #"\." "-") :value (or (:value rp-body) "")
               :description (:description rp-body) :acl {:owner {:principal "ADMIN"
                                                                 :type      "ROLE"}
                                                         :rules [{:principal user
                                                                  :type      "USER"
                                                                  :right     "MODIFY"}]}})))))
    ))
