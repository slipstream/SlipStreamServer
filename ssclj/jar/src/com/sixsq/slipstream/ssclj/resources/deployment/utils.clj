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
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [clojure.string :as string]))


(def ^:const deployment-resource-name "Deployment")

(def ^:const deployment-resource-url (u/de-camelcase deployment-resource-name))

(def edit-deployment-impl (std-crud/edit-fn deployment-resource-name))

(defn deployment-href-to-uuid [href] (string/replace-first href #"^deployment/" ""))

(defn update-deployment-attribut [deployment-uuid attribute-name value]
  (try
    (let [request {:params   {:resource-name deployment-resource-url
                              :uuid deployment-uuid}
                   :identity (create-identity-map ["super" #{"ADMIN"}])
                   :body     {(keyword attribute-name) value}}
          {:keys [status body]} (edit-deployment-impl request)]
      (case status
        200 (log/info "udpated deployment attribute: " body)
        (log/info "unexpected status code when udpating deployment resource:" status)))
    (catch Exception e
      (log/warn "error when updating deployment resource: " (str e) "\n"
                (with-out-str (st/print-cause-trace e))))))

(def ^:const deployment-parameter-resource-name "DeploymentParameter")

(def ^:const deployment-parameter-resource-url (u/de-camelcase deployment-parameter-resource-name))

(def ^:const deployment-parameter-resource-uri (str c/slipstream-schema-uri deployment-parameter-resource-name))

(def deployment-parameter-collection-acl {:owner {:principal "ADMIN"
                                                  :type      "ROLE"}
                                          :rules [{:principal "USER"
                                                   :type      "ROLE"
                                                   :right     "MODIFY"}]})

(def deployment-parameter-id-separator "_")

(defn deployment-parameter-id
  [{deployment-href :deployment-href node-name :node-name node-index :node-index name :name :as deployment-parameter}]
  (let [deployment-uuid (deployment-href-to-uuid deployment-href)]
    (cond
      (and deployment-uuid node-name node-index) (string/join deployment-parameter-id-separator [deployment-uuid node-name node-index name])
      (and deployment-uuid node-name) (string/join deployment-parameter-id-separator [deployment-uuid node-name name])
      deployment-uuid (string/join deployment-parameter-id-separator [deployment-uuid name]))))

(defn add-deployment-parameter-impl [{:keys [body] :as request}]
  (a/can-modify? {:acl deployment-parameter-collection-acl} request)
  (let [deployment-parameter (-> body
                                 u/strip-service-attrs
                                 (crud/new-identifier deployment-parameter-resource-name)
                                 (assoc :resourceURI deployment-parameter-resource-uri)
                                 u/update-timestamps
                                 (crud/add-acl request)
                                 crud/validate)
        response (db/add deployment-parameter-resource-name deployment-parameter {})
        value (:value body)
        node-path (zdu/deployment-parameter-znode-path body)]
    (uzk/create-all node-path :persistent? true)
    (uzk/set-data node-path value)
    (when (and (= "state-complete" (:name deployment-parameter))
               (= "node-instance" (:type deployment-parameter)))
      (uzk/create-all (zdu/deployment-parameter-node-instance-complete-state-znode-path deployment-parameter)))
    response))

(defn create-parameter [deployment-parameter]
  (try
    (let [request {:params   {:resource-name deployment-parameter-resource-url}
                   :identity (create-identity-map ["super" #{"ADMIN"}])
                   :body     deployment-parameter}
          {:keys [status body]} (add-deployment-parameter-impl request)]
      (case status
        201 (log/info "created deployment-parameter: " body)
        (log/info "unexpected status code when creating deployment-parameter resource:" status)))
    (catch Exception e
      (log/warn "error when creating deployment-parameter resource: " (str e) "\n"
                (with-out-str (st/print-cause-trace e))))))

(defn create-parameters [identity {nodes :nodes deployment-href :id state :state}]
  (let [user (:current identity)]
    (create-parameter
      {:deployment-href deployment-href :name "state" :value state :type "deployment"
       :acl             {:owner {:principal "ADMIN"
                                 :type      "ROLE"}
                         :rules [{:principal user
                                  :type      "USER"
                                  :right     "VIEW"}]}})
    (doseq [n nodes]
      (let [node-name (name (key n))
            multiplicity (read-string (get-in (val n) [:parameters :multiplicity :default-value] "1"))]
        (doseq [i (range 1 (inc multiplicity))]
          (create-parameter {:deployment-href deployment-href :node-name node-name :node-index i :type "node-instance"
                             :name            "state-complete" :value "" :acl {:owner {:principal "ADMIN"
                                                                                       :type      "ROLE"}
                                                                               :rules [{:principal user
                                                                                        :type      "USER"
                                                                                        :right     "MODIFY"}]}})
          (create-parameter {:deployment-href deployment-href :node-name node-name :node-index i :type "node-instance"
                             :name            "vmstate" :value "init" :acl {:owner {:principal "ADMIN"
                                                                                    :type      "ROLE"}
                                                                            :rules [{:principal user
                                                                                     :type      "USER"
                                                                                     :right     "MODIFY"}]}}))))))

