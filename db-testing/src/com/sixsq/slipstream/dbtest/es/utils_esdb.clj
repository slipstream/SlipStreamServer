(ns com.sixsq.slipstream.dbtest.es.utils-esdb
  (:require
    [com.sixsq.slipstream.db.es.binding :as esb]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.dbtest.es.utils :as esut]
    [com.sixsq.slipstream.db.impl :as db]))

;;
;; DB related.
;;

(defmacro with-test-es-client-and-db-impl
  "Creates an Elasticsearch test client, executes the body with the created
   client bound to the Elasticsearch client binding, and then clean up the
   allocated resources by closing both the client and the node."
  [& body]
  `(with-open [node#   (esut/create-test-node)
               client# (-> node#
                           esu/node-client
                           esb/wait-client-create-index)]
     (binding [esb/*client* client#]
       (db/set-impl! (esb/get-instance))
       (esu/reset-index esb/*client* esb/index-name)
       ~@body)))

(defn test-fixture-es-client-and-db-impl
  [f]
  (with-test-es-client-and-db-impl
    (f)))

;;
;; Following code is used to setup the Elasticsearch database client
;; and database CRUD implementation from Java code.
;;
;; ALL OF THESE FUNCTIONS ARE VERY STRONGLY DEPRECATED.
;;

(def ^:dynamic *es-server*)

(defn set-es-server!
  [server]
  (alter-var-root #'*es-server* (constantly server)))

(defn unset-es-server!
  []
  (.unbindRoot #'*es-server*))

(defn close-es-server!
  []
  (.close *es-server*)
  (unset-es-server!))

(defn ^{:deprecated "3.34"} set-db-crud-impl-uncond
  "STRONGLY DEPRECATED. Used to set the database CRUD implementation from Java
   code unconditionally. This must never be called from native clojure code."
  []
  (db/set-impl! (esb/get-instance)))

(defn ^{:deprecated "3.34"} set-db-crud-impl
  "STRONGLY DEPRECATED. Used to set the database CRUD implementation from Java
   code if needed. This must never be called from native clojure code."
  []
  (if (instance? clojure.lang.Var$Unbound db/*impl*)
    (set-db-crud-impl-uncond)))

;; Local test ES node.

(defn ^{:deprecated "3.34"} create-test-es-db-uncond
  "STRONGLY DEPRECATED. Used to set up a test Elasticsearch client from Java
   code unconditionally. This must never be called from native clojure code.
   Use of this function will cause a MEMORY LEAK."
  []
  (let [node   (esut/create-test-node)
        client (-> node
                   esu/node-client
                   esb/wait-client-create-index)]
    (set-es-server! node)
    (esb/set-client! client)))

(defn ^{:deprecated "3.34"} create-test-es-db
  "STRONGLY DEPRECATED. Used to set up a test Elasticsearch client from Java
   code if needed. This must never be called from native clojure code."
  []
  (if (instance? clojure.lang.Var$Unbound esb/*client*)
    (create-test-es-db-uncond)))

(defn ^{:deprecated "3.34"} test-db-client-and-crud-impl
  "STRONGLY DEPRECATED. Used to set up a test Elasticsearch client and the
   database CRUD implementation from Java code if needed. This must never be
   called from native clojure code."
  []
  (set-db-crud-impl)
  (create-test-es-db))

(defn ^{:deprecated "3.34"} test-db-unset-client-and-impl
  "STRONGLY DEPRECATED."
  []
  (esb/close-client!)
  (close-es-server!)
  (db/unset-impl!))
