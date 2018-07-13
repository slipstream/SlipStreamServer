(ns com.sixsq.slipstream.tools.cli.users-migration
  (:require
    [com.sixsq.slipstream.db.filter.parser :as parser]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.loader :as db-loader]
    [com.sixsq.slipstream.ssclj.resources.user-identifier]
    [com.sixsq.slipstream.ssclj.resources.user.user-identifier-utils :as uiu]
    [taoensso.timbre :as log]
    [clojure.string :as str])
  (:gen-class))

(def ^:private active-user-filter "(state='ACTIVE')")

(def default-db-binding-ns "com.sixsq.slipstream.db.es.loader")

(def ^:const hn-orgs #{"SixSq" "RHEA" "CERN" "CNRS" "DESY" "KIT" "INFN" "IFAE" "EMBL" "SURFSara"})

(def hn-usernames-to-ignore #{"super" "sixsq" "sixsq_prod" "julz"
                              "student01" "student02" "student03" "student04" "student05"
                              "student06" "student07" "student08" "student09" "student10"
                              "student11" "student12" "student13" "student14" "student15"
                              "student16" "student17" "student18" "student19" "student20"
                              "konstan_cotec" "konstan_gnss" "loomis" "test" "konstan"
                              "cjdcsixsq" "sixsq_dev" "mht" "sixsq_ci" "sebastien.fievet"
                              "rob" "elegoff" "cjdc" "meb" "LouMeri" "casdfafasf" "testuser"
                              "lionel" "doug"
                              "m.betti.rhea" "gbol16" "cedricseynat" "hnrp2"
                              "evamvako" "evamvak" "vamvakop"})


(defn init-db-client
  [binding-ns]
  (db-loader/load-and-set-persistent-db-binding binding-ns))


(defn- find-users
  [filter-str]
  (let [create-filter (fn [filter-string] {:filter (parser/parse-cimi-filter filter-string)})
        filter (create-filter filter-str)
        query-users (fn [f] (try
                              (second (db/query "user" {:cimi-params f
                                                        :user-roles  ["ADMIN"]}))
                              (catch Exception _ [])))]
    (query-users filter)))


(defn find-users-with-externalIdentities
  []
  (let [filter-str (format "externalIdentity!=null and %s" active-user-filter)
        matched-users (find-users filter-str)]
    matched-users))

(defn split-identifier
  "returns a tuple containing the 2 parts of an identifier"
  [identifier]
  (let [[instance login] (str/split identifier #":")]
    [instance login]))


(defn migrate-users-with-external-identity
  [users]
  (doseq [{:keys [username externalIdentity] :as u} users]
    (let [identifier-tuples (map split-identifier externalIdentity)]
      (doseq [[inst login] identifier-tuples]
        (log/debugf "Add User Identifier username = %s external-login = %s and instance  = %s" username login inst)
        (uiu/add-user-identifier! username nil login inst)))))


(defn find-users-with-githublogin
  []
  (let [filter-str (format "githublogin!=null and externalIdentity=null and %s" active-user-filter)
        matched-users (find-users filter-str)]
    matched-users))

(defn migrate-users-with-githublogin
  [users]
  (doseq [{:keys [username githublogin] :as u} users]
    (log/debugf "Add User Identifier username = %s authn-method=github and external-login = %s " username githublogin)
    (uiu/add-user-identifier! username "github" githublogin nil)))

(defn find-users-by-organization
  [org]
  (let [no-github-no-external-filter "githublogin=null and externalIdentity=null"
        filter-str (format "organization='%s' and %s and %s" org active-user-filter no-github-no-external-filter)
        matched-users (find-users filter-str)]
    matched-users))


(defn mangled-usernames-to-ignore
  "identify users who appear to be duplicated , "
  [users]
  (let [freq (frequencies (->> users (map :username) (map uiu/sanitize-login-name)))]
    (set (map first (filter #(> (second %) 1) freq)))))



(defn migrate-hn-users
  [users]
    (doseq [{:keys [username organization]} users]
      (log/debugf "username = %s method oidc and external-login=%s and instance = %s" username username (str/lower-case organization))
       (uiu/add-user-identifier! username nil username (str/lower-case organization))))

(defn migrate-biosphere-users
  [users]
    (doseq [{:keys [id username]} users]
      (when (not= username (-> id
                               (str/split #"/")
                               second
                               ))
        (log/warnf "username = %s but user-id = %s" username id))
      (log/debugf "username = %s method oidc and external-login=%s and instance = biosphere" username username)
      (uiu/add-user-identifier! username nil username "biosphere")))

(defn filter-hn-users
  [users]
  (let [blacklisted-usernames (clojure.set/union hn-usernames-to-ignore (mangled-usernames-to-ignore users))]
    (filter #(-> %
                 :username
                 blacklisted-usernames
                 not) users)))



(defn -main [& args]
  (let [_ (init-db-client default-db-binding-ns)
        extIdentity (find-users-with-externalIdentities)
        githublogin-users (find-users-with-githublogin)
        hn-users (-> (mapcat find-users-by-organization hn-orgs)
                     filter-hn-users)
        biosphere-users (find-users-by-organization "Biosphere")]
    (log/info "=== Migrating user identifiers ===")

    (log/debugf "----- %s users are found with an externalIdentity attribute " (count extIdentity))
    (log/debug "----------------------------------------------------------------")
    (migrate-users-with-external-identity extIdentity)

    (log/debugf "----- %s users are found with an githublogin attribute " (count githublogin-users))
    (log/debug "----------------------------------------------------------------")
    (migrate-users-with-githublogin githublogin-users)

    (log/debugf "----- %s users are found with a HnSciCloud organization " (count hn-users))
    (log/debug "----------------------------------------------------------------")
    (migrate-hn-users hn-users)

    (log/debugf "----- %s users are found with a BioSphere organization " (count biosphere-users))
    (log/debug "----------------------------------------------------------------")
    (migrate-biosphere-users biosphere-users)))
