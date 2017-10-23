;;
;; To run this script:
;; java -cp .../ssclj-XXX-SNAPSHOT-standalone.jar com.sixsq.slipstream.ssclj.migrate.user-cred
;;
(ns com.sixsq.slipstream.ssclj.migrate.user_cred
  (:require
    [korma.core :as kc]
    [korma.db :as kdb]
    [clojure.java.io :as io]
    [environ.core :as environ]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [sixsq.slipstream.client.sync :as sync]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [sixsq.slipstream.client.api.authn :as authn]
    [clojure.edn :as edn])
  (:gen-class))



(defn- find-resource
  [resource-path]
  (if-let [config-file (io/resource resource-path)]
    (do
      (println "================================================")
      (println (str "Will use " (.getPath config-file) " as config file"))
      config-file)
    (let [msg (str "Resource not found (must be in classpath): '" resource-path "'")]
      (println msg)
      (throw (IllegalArgumentException. msg)))))

(def default-db-spec
  {:classname   "org.hsqldb.jdbc.JDBCDriver"
   :subprotocol "hsqldb"
   :subname     "hsql://localhost:9001/slipstream"})

(def db-spec
  (if-let [config-name (environ/env :config-name)]
    (-> config-name
        find-resource
        slurp
        edn/read-string
        :api-db)
    (do
      (println (str "Using default db spec: " default-db-spec))
      default-db-spec)))

(defn- do-korma-init
  []
  (println "Will create korma database with db-spec")
  (println (u/map-multi-line db-spec))
  (kdb/defdb korma-api-db db-spec)
  (kc/defentity USERPARAMETER)
  (println "Korma database created"))

(defn- fetch-users
  [category]
  (kc/select USERPARAMETER
             (kc/fields "USERPARAMETER.*")
             (kc/where (= :USERPARAMETER.CATEGORY category))))


(defn extract-data
  [category coll k]
  (let [val (get coll k)
        byname (group-by :NAME val)
        key (:VALUE (first (get byname (str category ".username"))))
        secret (:VALUE (first (get byname (str category ".password"))))
        domain (:VALUE (first (get byname (str category ".domain.name"))))
        templateHref (cond
                       (= "exoscale-ch-gva" category) "credential-template/store-cloud-cred-exoscale"
                       (= "exoscale-ch-dk" category) "credential-template/store-cloud-cred-exoscale"
                       (= "open-telekom-de1" category) "credential-template/store-cloud-cred-otc")]
    (if (and key secret domain (not (empty? key)))
      {:credentialTemplate {:href        templateHref
                            :key         key
                            :secret      secret
                            :domain-name domain}
       :description        "Migration from database"
       :name               k})))

(defn do-credentials
  [client category]
  (let [
        login (authn/login client {:href     "session-template/internal"
                                   :username (environ/env :DBMIGRATION-USER)
                                   :password (environ/env :DBMIGRATION-PASSWORD)})
        get-grouped-data (fn [category] (group-by :CONTAINER_RESOURCEURI (fetch-users category)))
        coll (get-grouped-data category)
        templates (map (partial extract-data category coll) (keys coll))]
    (map #(cimi/add client "credentials" %) templates)))



(defn -main
  "Main function to migrate client data resources from DB to CIMI (Elastic Search)"
  []
  (let [init (do-korma-init)
        categories ["open-telekom-de1", "exoscale-ch-gva", "exoscale-ch-dk"]
        client (sync/instance)]
    (map #(do-credentials client %) categories)))

