(ns com.sixsq.slipstream.ssclj.resources.network-service-test
  (:require
    [clojure.test :refer :all]
    [peridot.core :refer :all]

    [com.sixsq.slipstream.ssclj.resources.network-service-schema-test :refer [valid-firewall]]
    [com.sixsq.slipstream.ssclj.resources.test-utils :refer [exec-request exec-post is-count]]
    [com.sixsq.slipstream.ssclj.resources.network-service :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]))

(def base-uri (str p/service-context resource-name))

(def invalid-firewall (dissoc valid-firewall :state))

(defn fixture-db-impl
  [f]
  (db/set-impl! (dbdb/get-instance))
  (f))

(use-fixtures :each fixture-db-impl)

(deftest post
  (testing "When not authenticated POSTing should be forbidden"
    (-> (exec-post base-uri nil valid-firewall)
        (t/is-status 403))
    (-> (exec-post base-uri "" valid-firewall)
        (t/is-status 403))
    (-> (exec-post base-uri "" invalid-firewall)
        (t/is-status 403)))

  (testing "When authenticated POSTing a valid representation should succeed"
    (-> (exec-post base-uri "joe" valid-firewall)
        (t/is-status 201)))

  (testing "When authenticated POSTing an valid representation should fail"
    (-> (exec-post base-uri "joe" invalid-firewall)
        (t/is-status 400))))


