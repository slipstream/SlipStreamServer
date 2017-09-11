(ns com.sixsq.slipstream.ssclj.resources.zk.deployment.utils
  (:require [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(def separator "/")
(def nodes-name "nodes")
(def lock-name "lock")

(defn deployment-path [deployment-href]
  (str separator deployment-href))

(defn lock-deployment-path [deployment-href]
  (str (deployment-path deployment-href) separator lock-name))

(defn deployment-parameter-path
  [{{deployment-href :href} :deployment node-name :node-name node-index :node-index name :name :as deployment-parameter}]
  (->> (cond
         (and node-name node-index)
         (string/join separator [nodes-name node-name node-index name])
         (and deployment-href node-name)
         (string/join separator [nodes-name node-name name])
         deployment-href name)
       (str (deployment-path deployment-href) separator)))



(defn deployment-parameter-node-instance-complete-state-path
  ([deployment-href node-name node-index parameter-name]
   (let [node-instance-complete-state-znode-id (string/join "_" [node-name node-index name])]
     (str separator
          (string/join separator [deployment-href "state" node-instance-complete-state-znode-id]))))
  ([{{deployment-href :href} :deployment node-name :node-name node-index :node-index name :name
     :as                     deployment-parameter}]
   (deployment-parameter-node-instance-complete-state-path deployment-href node-name node-index name)))

(defn lock-deployment
  "Create a lock for the deployment. This should be used each time multi-operations on zookeeper are needed to complete
  a request on deployment or deployment-parameter HTTP resource."
  [deployment-href]
  (uzk/create (lock-deployment-path deployment-href) :persistent? false))

(defn unlock-deployment
  "Release the lock for the specified deployment."
  [deployment-href]
  (uzk/delete (lock-deployment-path deployment-href)))

(defn is-deployment-locked?
  "Check if the deployment is locked."
  [deployment-href]
  (not (nil? (uzk/exists (lock-deployment-path deployment-href)))))

(defn check-deployment-lock-and-throw!
  "Throw if deployment is locked."
  [deployment-href]
  (when (is-deployment-locked? deployment-href)             ; TODO throw well known error code
    (throw (Exception. "Deployment is locked, come back later!"))))

(defn get-deployment-parameter-value [deployment-parameter & {:keys [watcher]}]
  (if watcher
    (uzk/get-data (deployment-parameter-path deployment-parameter) :watcher watcher)
    (uzk/get-data (deployment-parameter-path deployment-parameter))))

(defn deployment-state-path [deployment-href]
  (deployment-parameter-path {:deployment {:href deployment-href} :name "state"}))

(defn get-deployment-state [deployment-href]
  (uzk/get-data (deployment-state-path deployment-href)))

(defn check-same-state-and-throw!
  [deployment-href node-complete-state]
  (let [current-deployment-state (get-deployment-state deployment-href)]
    (when-not (= node-complete-state current-deployment-state)
      (throw (Exception.
               (str "State machine (complete-state = " node-complete-state
                    ") in different state from deployment state = " current-deployment-state "!"))))))

(defn complete-node-instance-state [{state :value :as deployment-parameter}]
  (uzk/delete (deployment-parameter-node-instance-complete-state-path deployment-parameter))
  (uzk/set-data (deployment-parameter-path deployment-parameter) state))

(defn all-nodes-completed-current-state? [deployment-href]
  (not (uzk/children (deployment-state-path deployment-href))))
