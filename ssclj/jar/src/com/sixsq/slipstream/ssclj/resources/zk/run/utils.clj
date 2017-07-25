(ns com.sixsq.slipstream.ssclj.resources.zk.run.utils
  (:require [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
            [zookeeper :as zk]
            [clojure.string :as string]))

(def znode-separator "/")

(def nodes-txt "nodes")

(defn parameter-znode-path [run-id node-name node-index name]
  (->> (cond
         (and run-id node-name node-index) (string/join znode-separator [run-id nodes-txt node-name node-index name])
         (and run-id node-name) (string/join znode-separator [run-id nodes-txt node-name name])
         run-id (string/join znode-separator [run-id name]))
       (str znode-separator)))

(defn run-parameter-znode-path
  [{run-id :run-id node-name :node-name node-index :node-index name :name :as run-parameter}]
  (parameter-znode-path run-id node-name node-index name))

;(defn run-id-path [run-id])

;  (str runs-path "/" run-id))
;
;(defn nodes-path [run-id]
;  (str (run-id-path run-id) "/nodes"))
;
;(defn node-path
;  [run-id node-name]
;  (str (nodes-path run-id) "/" node-name))
;
;(defn indexed-node-path [run-id node-name node-index]
;  (str (node-path run-id node-name) "/" node-index))
;
;(defn create-run [run-id]
;  (uzk/create (run-id-path run-id) :persistent? true))



#_(defn create-zk-run [{run-id :id nodes :nodes :as run}]
  (doseq [n nodes]
    (zk/create-all uzk/*client* (node-path run-id (key n)) :persistent? true)
    (let [{:keys [params multiplicity mapping]} (val n)]
      (doseq [i (range 1 (inc multiplicity))]
        (zk/create-all client (rzu/params-znode-path run-id (key n) i) :persistent? true)
        (doseq [p params]
          (if-let [default (-> p val :default)]
            (zk/create client (rzu/param-znode-path run-id (key n) i (key p))
                       :data (zdata/to-bytes default)
                       :persistent? true)
            (zk/create client (rzu/param-znode-path run-id (key n) i (key p))
                       :persistent? true))))))
  (rsm/create client run-id module params)
  )