(ns com.sixsq.slipstream.ssclj.api.acl
  (:require
    [com.sixsq.slipstream.ssclj.api.korma-helper  :as kh]
    [clojure.java.jdbc :refer :all                :as jdbc]
    [clojure.tools.logging                        :as log]
    [clojure.set                                  :as s]
    [clojure.walk                                 :as w]
    [korma.core                                   :refer :all]
    [com.sixsq.slipstream.ssclj.api.ddl           :as ddl])
  (:gen-class
    :name com.sixsq.slipstream.ssclj.api.Acl
    :methods [

    #^{:static true} [init            [] void]

    #^{:static true} [insertResource  [String String java.util.Map] int]
    #^{:static true} [getResourceIds  [String java.util.Map] java.util.Set]
    #^{:static true} [hasResourceId   [String String java.util.Map] boolean]
    #^{:static true} [deleteResource  [String String java.util.Map] boolean]

    ;;
    ;; Utils functions
    ;;
    #^{:static true} [idMap           [String java.util.List] java.util.Map]

    ;; TODO st protect in some environments
    #^{:static true} [deleteAll       [] void]
    #^{:static true} [populate        [String String int] void]]))

(defn create-entities   
  []
  (defentity acl)
  (select acl (limit 1))) ;; korma "warmup"
    
;; 
;;
(def do-init
  (delay
    (kh/korma-init)
    (ddl/create-ddl)
    (create-entities)
    (log/info "DB successfully setup")))

(defn -init
  []
  @do-init)
;; 
;;

;;
;;
(defn- check-not-empty   
  [type-names]
  (if (empty? type-names)
    (throw (IllegalArgumentException. "Can not be called with an empty set"))))

(defn- rows
  ([id type type-names]    
    (check-not-empty type-names)
    (set (for [[ptype pname] type-names] {:resource-id id :resource-type type :principal-type ptype :principal-name pname})))
  ([type type-names]
    (check-not-empty type-names)
    (set (for [[ptype pname] type-names] {:resource-type type :principal-type ptype :principal-name pname}))))    

;;
;; An entire row is also the clause to query it in db
(defn- exists? [row] (seq (select acl (where row))))

(defn- filter-existing [rows] (filter exists? rows))

(defn- log-warn-existing
  [existings]
  (doseq [existing existings]
    (log/warn (str "Row already in database '" existing "'"))))

(defn- check-init-called
  []
  (when-not (realized? do-init)
    (throw (IllegalStateException. "init must have been be called before accessing database"))))

(defn- parse-id-map
  "Parses
  { :identity \"joe\"
    :roles [\"ROLE1\" \"ROLE2\"] }
    into 
  a vector of 2-tuples (type name) 
 [ [\"user\" \"joe\"] [\"role\" \"ROLE1\"] [\"role\" \"ROLE2\"] ]"
  [authn]
  (if (empty? authn)
    (throw (IllegalArgumentException. "Can not be called with an empty map")))
    (let [user-name (:identity authn)
          role-names (:roles authn)]
      (concat [["user" user-name]] (map (fn[r] ["role" r]) role-names))))

(defn extract-current   
  [authns]
  (if-let [current (:current authns)]
    (get-in authns [:authentications (keyword current)])
    authns))

(defn parse-authn   
  [authn-map]
  (->> authn-map
    (into {})
    w/keywordize-keys
    extract-current
    parse-id-map))

;; API
;;

(defn -idMap   
  [user roles]
  {:identity user :roles roles})

(defn -insertResource
  [^String id ^String type ^java.util.Map authn]
  (check-init-called)
  (let [
    principals (parse-authn authn)
    candidates (rows id type principals)
    existings (filter-existing candidates)
    actual-inserts (s/difference candidates existings)]    

    (when (seq actual-inserts)
      ;; loop of single inserts instead of bulk one because korma translates this
      ;; into SQL not supported by sqlite : http://stackoverflow.com/questions/1609637/is-it-possible-to-insert-multiple-rows-at-a-time-in-an-sqlite-database
      (doseq [actual-insert actual-inserts]        
        (insert acl (values actual-insert))))

    (log-warn-existing existings)
    (count candidates)))

(defn -getResourceIds
  [^String type ^java.util.Map authn]
  (check-init-called)  
  (->> (select acl
          (where (apply or (rows type (parse-authn authn))))
          (fields [:resource-id]))
    (map :resource-id)
    (into #{})))

(defn -hasResourceId
  [^String id ^String type ^java.util.Map authn]
  (check-init-called)
  (not (empty? (select acl (where (apply or (rows id type (parse-authn authn))))))))
  
(defn -deleteResource
  [^String id ^String type ^java.util.Map authn]  
  (check-init-called) 
  (delete acl (where (apply or (rows id type (parse-authn authn)))))
  true)
  
;; Convenience functions that support tests
;;
(def ^:dynamic *bulk-size* 5000)

(defn delete-all 
  [] 
  (check-init-called)  
  (delete acl)
  (log/info "All ACL deleted"))

(defn- row
  [type user i]
  {:resource-id (str type"/"i) :principal-type "USER" :principal-name (str user i)})

(defn- bulk
  [type user start size]
  (map
    (partial row type user)
    (range start (+ start size))))

(defn populate
  [type user nb]
  (assert (> nb *bulk-size*) (str "Bulk populate works with at least "*bulk-size* " elements"))
  (println "Will populate " nb  "elements, "type"/"user)
  (let [nb-bulks (/ nb *bulk-size*)]
    (doseq [i (range 1 (inc nb-bulks))]
      (println (* i *bulk-size*) " " (int (* 100 (/ i nb-bulks))) "%" )
      (insert acl (values (bulk type user (* (dec i) *bulk-size*) *bulk-size*))))))

;; Java API

(defn -deleteAll [] (delete-all))

(defn -populate
  [type user nb]
  (populate type user nb))

