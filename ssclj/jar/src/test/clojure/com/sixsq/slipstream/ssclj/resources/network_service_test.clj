(ns com.sixsq.slipstream.ssclj.resources.network-service-test
  (:require
    [clojure.test :refer :all]
    [peridot.core :refer :all]

    [korma.core :as kc]

    [com.sixsq.slipstream.ssclj.resources.network-service-schema-test :refer [valid-firewall]]
    [com.sixsq.slipstream.ssclj.resources.test-utils :refer [exec-request exec-post is-count]]
    [com.sixsq.slipstream.ssclj.resources.network-service :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]))

(def base-uri (str p/service-context resource-name))

(defn fixture-db-impl
  [f]
  (db/set-impl! (dbdb/get-instance))
  (f))

(defn fixture-delete-all
  [f]
  (kc/delete dbdb/resources)
  (f))

(use-fixtures :once fixture-db-impl)
(use-fixtures :each fixture-delete-all)

(def valid-create-firewall
  (dissoc valid-firewall :id :created :updated))

(def invalid-create-firewall (dissoc valid-create-firewall :state))

(deftest add-collection
  (testing "When not authenticated, POSTing should be forbidden"
    (-> (exec-post base-uri nil valid-create-firewall)
        (t/is-status 403))
    (-> (exec-post base-uri "" valid-create-firewall)
        (t/is-status 403)))

  (testing "When authenticated, POSTing a valid representation should succeed"
    (-> (exec-post base-uri "joe" valid-create-firewall)
        (t/is-status 201)))

  (testing "POSTing an invalid representation should fail"
    (-> (exec-post base-uri "john" invalid-create-firewall)
        (t/is-status 400)))
    (-> (exec-post base-uri "" invalid-create-firewall)
        (t/is-status 400)))

(deftest query-collection
  (testing "GETing the collection"
    (exec-post base-uri "john" valid-create-firewall)
    (-> (exec-request base-uri "" "")
        (t/is-status 403))
    (-> (exec-request base-uri "" "jack")
        (t/is-status 200)
        (t/is-key-value :count 0))
    (-> (exec-request base-uri "" "john")
        (t/is-status 200)
        (t/is-key-value :count 1))))



