(ns com.sixsq.slipstream.tools.cli.users-migration
  (:require
    [com.sixsq.slipstream.db.filter.parser :as parser]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.loader :as db-loader]
    [taoensso.timbre :as log])
  (:gen-class)
  )

(def ^:private active-user-filter "(state='ACTIVE')")

(def default-db-binding-ns "com.sixsq.slipstream.db.es.loader")

(def ^:const hn-orgs #{"SixSq" "RHEA" "CERN" "CNRS" "DESY"  "KIT" "INFN" "IFAE" "EMBL" "SURFSara"})
(def ^:const biosphere-orgs #{"IFB" "Biosphere"})


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


(defn find-users-with-githublogin
  []
  (let [filter-str (format "githublogin!=null and %s" active-user-filter)
        matched-users (find-users filter-str)]
    matched-users))


(defn find-users-by-organization
  [org]
  (let [filter-str (format "organization='%s' and %s" org active-user-filter)
        matched-users (find-users filter-str)
        ]
    matched-users
    ))

  (defn filter-not-in-orgs
    [orgs]
    (str
      (subs (->> orgs
                 (map #(str " and organization!='" % "'"))
                 (concat)
                 (apply str)) 5)
      " and " active-user-filter))

  (defn find-non-other-long-names
    []
    (let [filter-str (str (filter-not-in-orgs hn-orgs)
                          " and  " (filter-not-in-orgs biosphere-orgs)
                          " and externalIdentity=null"
                          " and githublogin=null"
                          " and " active-user-filter)
          non-hn-users (find-users filter-str)
          long-name? (fn [user] (< 20
                                   (-> user
                                       :username
                                       count
                                       )))
          matched-users (filter long-name? non-hn-users)]
      (doseq [u matched-users]
        (log/debugf "Username %s belongs to organization %s" (:username u) (:organization u))
        )
      matched-users
      ))


(defn print-users
  [users]
  (doseq [u users]
    (clojure.pprint/pprint (str (:username u) " (" (:firstName u)" " (:lastName u)")  => " (:organization u)))
    )
  )


  (defn -main [& args]
    (let [_ (init-db-client default-db-binding-ns)
          extIdentity (find-users-with-externalIdentities)
          githublogin (find-users-with-githublogin)
          hn-users (mapcat find-users-by-organization hn-orgs)
          biosphere-users (mapcat find-users-by-organization biosphere-orgs)
          remaining (find-non-other-long-names)
          ]


      (log/debug "Migrating user identifiers")
      (log/debugf "%s users are found with an externalIdentity attribute " (count extIdentity))
      (log/debugf "%s users are found with an githublogin attribute " (count githublogin))
      (log/debugf "%s users are found with a HnSciCloud organization " (count hn-users))
      (log/debugf "%s users are found with a BioSphere organization " (count biosphere-users))
      (log/debugf "%s users are left with other organization and long username" (count remaining))

      ))


