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


(defn check-exo-template
  [t u]
  (let [
        {key :key secret :secret domain :domain-name connector :connector} (:credentialTemplate t)]
    (if (and key secret domain connector (not (empty? key))) (assoc t :user u))))


(defn check-otc-template
  [t u]
  (let [
        {key :key secret :secret domain :domain-name tenant :tenant-name connector :connector} (:credentialTemplate t)]
    (if (and key secret domain tenant connector (not (empty? key))) (assoc t :user u))))

(defn extract-data
  [category coll user]
  (let [val (get coll user)
        byfields (group-by :NAME val)
        key (:VALUE (first (get byfields (str category ".username"))))
        secret (:VALUE (first (get byfields (str category ".password"))))
        domain (:VALUE (first (get byfields (str category ".domain.name"))))
        tenant (:VALUE (first (get byfields (str category ".tenant.name"))))
        exo-template {:credentialTemplate {:href        "credential-template/store-cloud-cred-exoscale"
                                           :key         key
                                           :secret      secret
                                           :connector   (str "connector/" category)
                                           :domain-name domain}}
        otc-template {:credentialTemplate {:href        "credential-template/store-cloud-cred-otc"
                                           :key         key
                                           :secret      secret
                                           :tenant-name tenant
                                           :connector   (str "connector/" category)
                                           :domain-name domain}}]
    (cond
      (= "exoscale-ch-gva" category) (check-exo-template exo-template user)
      (= "exoscale-ch-dk" category) (check-exo-template exo-template user)
      (= "open-telekom-de1" category) (check-otc-template otc-template user))))


(defn merge-acl
  [v]

  (let [users (map :user v)
        add-rule (fn [u] {:type      "USER"
                          :principal u
                          :right     "VIEW"
                          })
        rules (vec (map add-rule users))
        base-acl {:owner {:principal "ADMIN"
                          :type      "ROLE"}}
        base-rule {
                   :type      "ROLE",
                   :principal "ADMIN",
                   :right     "ALL"
                   }
        ]
    (assoc base-acl :rules (conj rules base-rule))))


(defn generate-records
  [vt]
  (let [header (dissoc (first (first vt)) :user)
        gen-content (fn [v] (assoc-in header [:credentialTemplate :acl] (merge-acl v)))]
    (map gen-content vt)))

(defn add-credentials
  [client category]
  (let [

        get-grouped-data (fn [c] (group-by :CONTAINER_RESOURCEURI (fetch-users c)))
        coll (get-grouped-data category)
        templates (filter (complement nil?) (map (partial extract-data category coll) (keys coll)))
        templates-by-key (group-by #(:key (:credentialTemplate %)) templates)
        records (generate-records (map second templates-by-key))
        ]

    (println (str "Collection for " category " : Adding " (count coll) " records"))
    (println (str "Migrating category " category " : Adding " (count records) " credentials"))
    (map (partial cimi/add client "credentials") records)))

(defn -main
  " Main function to migrate client data resources from DB to CIMI (Elastic Search) "
  []
  (let [init (do-korma-init)
        categories #{"open-telekom-de1", "exoscale-ch-gva", "exoscale-ch-dk"}
        cep-endpoint (or (environ/env :dbmigration-endpoint) "https://nuv.la/api/cloud-entry-point")
        client (if (environ/env :dbmigration-options) (sync/instance cep-endpoint (read-string (environ/env :dbmigration-options))) (sync/instance cep-endpoint))
            login (authn/login client {:href     "session-template/internal"
                                       :username (environ/env :dbmigration-user) ;;export DBMIGRATION_USER="super"

                                       :password (environ/env :dbmigration-password)})]

           (map (partial add-credentials client) categories)

           ))

