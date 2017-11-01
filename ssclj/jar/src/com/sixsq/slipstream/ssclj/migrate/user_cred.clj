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
    [sixsq.slipstream.client.api.authn :as authn]
    [clojure.edn :as edn])
  (:gen-class))

(def ^:const keys-cred-full [:href :key :secret :connector :domain-name :tenant-name])
(def ^:const keys-cred-nuvlabox (vec (remove #{:domain-name :tenant-name} keys-cred-full)))
(def ^:const keys-cred-stratuslab (vec (remove #{:domain-name :tenant-name} keys-cred-full)))
(def ^:const keys-cred-otc keys-cred-full)
(def ^:const keys-cred-openstack keys-cred-full)
(def ^:const keys-cred-opennebula (vec (remove #{:domain-name :tenant-name} keys-cred-full)))
(def ^:const keys-cred-exoscale (vec (remove #{:tenant-name} keys-cred-full)))
(def ^:const keys-cred-ec2 (vec (remove #{:domain-name :tenant-name} keys-cred-full)))


(def ^:const cat-nuvlabox #{"nuvlabox-albert-einstein", "nuvlabox-arthur-harden", "nuvlabox-bertil-ohlin",
                            "nuvlabox-carl-cori", "nuvlabox-cecil-powell", "nuvlabox-christiane-n-volhard",
                            "nuvlabox-christiane-nusslein-volhard", "nuvlabox-demo", "nuvlabox-felix-bloch",
                            "nuvlabox-henry-dunant", "nuvlabox-james-chadwick", "nuvlabox-joseph-e-murray",
                            "nuvlabox-joseph-e-stiglitz", "nuvlabox-joseph-erlanger", "nuvlabox-joseph-h-taylor-jr",
                            "nuvlabox-joseph-l-goldstein", "nuvlabox-jules-bordet", "nuvlabox-max-born",
                            "nuvlabox-max-planck", "nuvlabox-scissor1", "nuvlabox-scissor2", "nuvlabox-stanley-cohen",
                            "nuvlabox-yves-chauvin"})

(def ^:const cat-stratuslabiter #{"atos-es1"})
(def ^:const cat-otc #{"open-telekom-de1"})
(def ^:const cat-openstack #{"advania-se1", "cyclone-de1", "cyclone-fr2", "cyclone-tb-it1", "ebi-embassy-uk1",
                             "eo-cloudferro-pl1", "ifb-bird-stack", "ifb-bistro-iphc", "ifb-core-cloud", "ifb-core-pilot",
                             "ifb-genouest-genostack"})
(def ^:const cat-opennebula #{"eo-cesnet-cz1", "scissor-fr1", "scissor-fr2", "scissor-fr3", "teidehpc-es-tfs1"})
(def ^:const cat-exoscale #{"exoscale-ch-dk", "exoscale-ch-gva"})
(def ^:const cat-ec2 #{"ec2-ap-northeast-1", "ec2-ap-southeast-1", "ec2-ap-southeast-2", "ec2-eu-central-1",
                       "ec2-eu-west", "ec2-eu-west-2", "ec2-sa-east-1", "ec2-us-east-1", "ec2-us-west-1", "ec2-us-west-2"})



(def ^:const mappings-nuvlabox (set (map #(hash-map % {:ks keys-cred-nuvlabox :template-name "store-cloud-cred-nuvlabox"}) cat-nuvlabox)))
(def ^:const mappings-stratuslabiter (set (map #(hash-map % {:ks keys-cred-stratuslab :template-name "store-cloud-cred-stratuslabiter"}) cat-stratuslabiter)))
(def ^:const mappings-otc (set (map #(hash-map % {:ks keys-cred-otc :template-name "store-cloud-cred-otc"}) cat-otc)))
(def ^:const mappings-openstack (set (map #(hash-map % {:ks keys-cred-openstack :template-name "store-cloud-cred-openstack"}) cat-openstack)))
(def ^:const mappings-opennebula (set (map #(hash-map % {:ks keys-cred-opennebula :template-name "store-cloud-cred-opennebula"}) cat-opennebula)))
(def ^:const mappings-exoscale (set (map #(hash-map % {:ks keys-cred-exoscale :template-name "store-cloud-cred-exoscale"}) cat-exoscale)))
(def ^:const mappings-ec2 (set (map #(hash-map % {:ks keys-cred-ec2 :template-name "store-cloud-cred-ec2"}) cat-ec2)))


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
  "return true if template t is valid"
  [t ks]
  (and
    (not (nil? t))
    (not (nil? ks))
    (not (empty? ks))
    (vector? ks)
    (map? t)
    (contains? t :credentialTemplate)
    ;;every key is ks must have non nil values in template t
    (every? (complement nil?) (map #(% (:credentialTemplate t)) (map #(keyword %) ks)))))

(defn extract-data
  [category coll user]
  (let [byfields (group-by :NAME (get coll user))
        get-value (fn [field] (:VALUE (first (get byfields (str category field)))))
        key (get-value ".username")
        secret (get-value ".password")
        domain (get-value ".domain.name")
        tenant (get-value ".tenant.name")
        base-template {:credentialTemplate {:href        (str "credential-template/" (-> (mapped category)
                                                                                         :template-name))
                                            :key         key
                                            :secret      secret
                                            :tenant-name tenant
                                            :connector   (str "connector/" category)
                                            :domain-name domain}}

        template-keys (:ks (mapped category))
        template-instance (update-in base-template [:credentialTemplate] select-keys template-keys)]

    (when (valid-template? template-instance template-keys)
      (assoc template-instance :user user))))


(defn merge-acl
  [v]
  (let [users (map :user v)
        add-rule (fn [u] (when u {:type      "USER"
                                  :principal u
                                  :right     "VIEW"
                                  }))
        user-rules (remove nil? (map add-rule users))
        base-acl {:owner {:principal "ADMIN"
                          :type      "ROLE"}}
        base-rule [{:type      "ROLE",
                    :principal "ADMIN",
                    :right     "ALL"}]]
    (assoc base-acl :rules (reduce conj base-rule user-rules))))


(defn generate-records
  [vt]
  {:pre [;; non empty sequence
         (seq? vt)
         (not (empty? vt))
         ;;containing only vectors
         (every? vector? vt)
         ;;every of those vectors contain map
         (every? true? (map #(every? map? %) vt))]}
  (let [header (dissoc (first (first vt)) :user)
        gen-content (fn [v] (assoc-in header [:credentialTemplate :acl] (merge-acl v)))]
    (map gen-content vt)))

(defn add-credentials
  [client category]
  (let [get-grouped-data (fn [c] (group-by :CONTAINER_RESOURCEURI (fetch-users c)))
        coll (get-grouped-data category)
        templates (remove nil? (map (partial extract-data category coll) (keys coll)))
        templates-by-key (group-by #(:key (:credentialTemplate %)) templates)
        records (generate-records (map second templates-by-key))]
    (println (str "Collection for " category " : Adding " (count coll) " records"))
    (println (str "Migrating category " category " : Adding " (count records) " credentials"))
    (map (partial cimi/add client "credentials") records)))


(defn -main
  " Main function to migrate client data resources from DB to CIMI (Elastic Search) "
  []
  (let [init (do-korma-init)
        categories (keys mappings)
        cep-endpoint (or (environ/env :dbmigration-endpoint) "https://nuv.la/api/cloud-entry-point")
        ;;e.g DBMIGRATION_OPTIONS={:insecure? true}
        client (if (environ/env :dbmigration-options) (sync/instance cep-endpoint (read-string (environ/env :dbmigration-options))) (sync/instance cep-endpoint))
        login (authn/login client {:href     "session-template/internal"
                                   :username (environ/env :dbmigration-user) ;;export DBMIGRATION_USER="super"

                                   :password (environ/env :dbmigration-password)})]
    (map (partial add-credentials client) (keys mappings))))

