(ns com.sixsq.slipstream.ssclj.app.test-server
  (:require
    [com.sixsq.slipstream.db.es.binding :as esb]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as zku]
    [com.sixsq.slipstream.ssclj.app.server :as server]
    [metrics.core :refer [remove-all-metrics]]
    [com.sixsq.slipstream.db.impl :as db]
    [zookeeper :as zk])
  (:import (org.apache.curator.test TestingServer)))


(def ^:dynamic *service-clients* nil)


(defn set-service-clients [m]
  (alter-var-root #'*service-clients* (constantly m)))


(def ^:dynamic *stop-server-fn* nil)


(defn set-stop-server-fn [f]
  (alter-var-root #'*stop-server-fn* (constantly f)))


(defn create-clients []
  (let [zk-port 12001
        zk-server (TestingServer. zk-port)
        zk-client (zk/connect (str "127.0.0.1:" zk-port))
        zk-create-client-fn (constantly zk-client)

        es-port 12002
        es-node (esu/create-test-node)
        es-client (-> es-node
                      esu/node-client
                      esb/wait-client-create-index)
        es-create-client-fn (constantly es-client)]
    {:zk-port             zk-port
     :zk-server           zk-server
     :zk-client           zk-client
     :zk-create-client-fn zk-create-client-fn

     :es-port             es-port
     :es-node             es-node
     :es-client           es-client
     :es-create-client-fn es-create-client-fn}))


(defn start-clients []
  (set-service-clients (create-clients)))


(defn stop-clients []
  (let [{:keys [zk-server zk-client es-node es-client]} *service-clients*]
    (set-service-clients nil)
    (try
      (zku/close-client!)
      (finally
        (try
          (.close zk-server)
          (finally
            (try
              (.close es-client)
              (finally
                (.close es-node)))))))))


(defn start []
  (start-clients)
  (let [ssclj-port 12003
        {:keys [zk-create-client-fn es-create-client-fn es-client]} *service-clients*]
    (with-redefs [esb/create-client es-create-client-fn
                  zku/create-client zk-create-client-fn]
      (set-stop-server-fn (server/start ssclj-port)))
    (esb/set-client! es-client)
    (db/set-impl! (esb/get-instance))
    (System/setProperty "ssclj.endpoint" (str "http://localhost:" ssclj-port))))


(defn stop []
  (when-let [stop-fn *stop-server-fn*]
    (set-stop-server-fn nil)
    (try
      (stop-fn)
      (catch Exception _))
    (stop-clients)
    (esb/unset-client!)
    (db/unset-impl!)
    (System/clearProperty "ssclj.endpoint")
    (remove-all-metrics)))


(defn refresh-es-indices
  []
  (esu/refresh-all-indices (:es-client *service-clients*)))
