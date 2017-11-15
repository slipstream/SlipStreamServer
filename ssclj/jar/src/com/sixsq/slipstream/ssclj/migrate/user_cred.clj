;;
;; To run this script:
;; java -cp .../ssclj-XXX-SNAPSHOT-standalone.jar com.sixsq.slipstream.ssclj.migrate.user-cred
;;
(ns com.sixsq.slipstream.ssclj.migrate.user-cred
  (:require
    [korma.core :as kc]
    [korma.db :as kdb]
    [clojure.java.io :as io]
    [environ.core :as environ]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [sixsq.slipstream.client.sync :as sync]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.config :as uc]
    [sixsq.slipstream.client.api.authn :as authn]
    [clojure.string :as str]
    [clojure.edn :as edn])
  (:gen-class))

(def ^:const cat-config (uc/read-config (or (environ/env :dbmigration-configfile)
                                            "com/sixsq/slipstream/migrate/credential-categories-config.edn")))

(defn gen-mappings
  [category t]
  (set (map #(hash-map %
                       {:ks            (-> cat-config
                                           category
                                           :template-keys)
                        :template-name t})
            (when category
              (-> cat-config
                  category
                  :connectors)))))


(def ^:const mappings-nuvlabox (gen-mappings :cat-nuvlabox "store-cloud-cred-nuvlabox"))
(def ^:const mappings-stratuslabiter (gen-mappings :cat-stratuslabiter "store-cloud-cred-stratuslabiter"))
(def ^:const mappings-otc (gen-mappings :cat-otc "store-cloud-cred-otc"))
(def ^:const mappings-openstack (gen-mappings :cat-openstack "store-cloud-cred-openstack"))
(def ^:const mappings-opennebula (gen-mappings :cat-opennebula "store-cloud-cred-opennebula"))
(def ^:const mappings-exoscale (gen-mappings :cat-exoscale "store-cloud-cred-exoscale"))
(def ^:const mappings-ec2 (gen-mappings :cat-ec2 "store-cloud-cred-ec2"))

(def ^:const mappings
  (clojure.set/union mappings-nuvlabox
                     mappings-stratuslabiter
                     mappings-otc
                     mappings-openstack
                     mappings-opennebula
                     mappings-exoscale
                     mappings-ec2))

(defn mapped
  "Return characteristics of given category as defined in `mappings`"
  [k]
  (->> (map #(get % k) mappings)
       (remove nil?)
       first))

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

(defn valid-template?
  "return true if template t is valid, ks is the list of mandatory keys"
  [t ks]
  (and
    (not (nil? t))
    (not (nil? ks))
    (seq ks)
    (vector? ks)
    (map? t)
    (contains? t :credentialTemplate)
    ;; key and secret are mandatory in any case
    (contains? (:credentialTemplate t) :key)
    (contains? (:credentialTemplate t) :secret)
    ;; key and secret shall not be empty
    (seq (-> t :credentialTemplate :key))
    (seq (-> t :credentialTemplate :secret))
    ;; either tenant is defined and not empty,or it is undefined
    (or (nil? (-> t :credentialTemplate :tenant-name)) (seq (-> t :credentialTemplate :tenant-name)))
    ;;every key is ks must have non nil values in template t
    (every? (complement nil?) (map #(% (:credentialTemplate t)) (map keyword ks)))))

(defn extract-data
  [category coll user]
  (let [byfields (group-by :NAME (get coll user))
        get-value (fn [field] (:VALUE (first (get byfields (str category field)))))
        key (or (get-value ".username") (get-value ".access.id"))
        secret (or (get-value ".password") (get-value ".secret.key"))
        domain (get-value ".domain.name")
        tenant (get-value ".tenant.name")
        base-template {:credentialTemplate {:href        (str "credential-template/" (-> (mapped category)
                                                                                         :template-name))
                                            :key         key
                                            :secret      secret
                                            :tenant-name tenant
                                            :connector   (str "connector/" category)
                                            :domain-name domain}
                       :name               category}

        add-acl-to-template (fn [t u] (when (and t u) (assoc-in t [:credentialTemplate :acl] {:owner {:principal (str/replace u #"^user/" "")
                                                                                                      :type      "USER"}
                                                                                              :rules [{:type      "ROLE",
                                                                                                       :principal "ADMIN",
                                                                                                       :right     "ALL"}
                                                                                                      {:type      "USER",
                                                                                                       :principal (str/replace u #"^user/" ""),
                                                                                                       :right     "MODIFY"}
                                                                                                      ]})))
        template-keys (:ks (mapped category))
        template-instance (update-in base-template [:credentialTemplate] select-keys template-keys)]
    (when (valid-template? template-instance template-keys) (add-acl-to-template template-instance user))))

(defn add-credentials
  [client category]
  (let [get-grouped-data (fn [c] (group-by :CONTAINER_RESOURCEURI (fetch-users c)))
        coll (get-grouped-data category)
        users (keys coll)
        records (remove nil? (map (partial extract-data category coll) users))
        ]
    (println (str "Collection for " category " : Adding " (count coll) " records"))
    (println (str "Migrating category " category " : Adding " (count records) " credentials"))
    (doall (map (partial cimi/add client "credentials") records))))

(defn -main
  " Main function to migrate client data resources from DB to CIMI (Elastic Search) "
  []
  (let [init (do-korma-init)
        ;categories (keys mappings)
        cep-endpoint (or (environ/env :dbmigration-endpoint) "https://nuv.la/api/cloud-entry-point")
        ;;e.g DBMIGRATION_OPTIONS={:insecure? true}
        client (if (environ/env :dbmigration-options) (sync/instance cep-endpoint (read-string (environ/env :dbmigration-options))) (sync/instance cep-endpoint))
        login (authn/login client {:href     "session-template/internal"
                                   :username (environ/env :dbmigration-user) ;;export DBMIGRATION_USER="super"

                                   :password (environ/env :dbmigration-password)})]
    (doall (map (partial add-credentials client) (map #(first (keys %)) mappings)))))
