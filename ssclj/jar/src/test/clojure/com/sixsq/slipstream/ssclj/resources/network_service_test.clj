(ns com.sixsq.slipstream.ssclj.resources.network-service-test
  (:require
    [clojure.test                                                     :refer :all]
    [peridot.core                                                     :refer :all]

    [korma.core                                                       :as kc]

    [com.sixsq.slipstream.ssclj.api.acl                               :as acl]
    [com.sixsq.slipstream.ssclj.resources.network-service-schema-test :refer [valid-firewall]]
    [com.sixsq.slipstream.ssclj.resources.test-utils                  :refer [exec-request exec-post is-count]]
    [com.sixsq.slipstream.ssclj.resources.network-service             :refer :all]
    [com.sixsq.slipstream.ssclj.app.params                            :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils        :as t]
    [com.sixsq.slipstream.ssclj.db.impl                               :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding                   :as dbdb]
    [com.sixsq.slipstream.ssclj.resources.common.utils                :as u]))

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn fixture-set-impl
  [f]
  (db/set-impl! (dbdb/get-instance))
  (f))

(defn fixture-delete-all
  [f]
  (kc/delete acl/acl)
  (kc/delete dbdb/resources)
  (f))

(use-fixtures :once fixture-set-impl)
(use-fixtures :each fixture-delete-all)

(def valid-create-firewall
  (dissoc valid-firewall :id :created :updated))
(def invalid-create-firewall (assoc valid-create-firewall :state "INVALID STATE"))
(def updated-firewall
  (assoc valid-create-firewall :state "STOPPED"))
(def partial-firewall {:state "ERROR"})


(deftest add-collection
  (testing "When not authenticated, adding should be forbidden"
    (-> (exec-post base-uri nil valid-create-firewall)
        (t/is-status 403))
    (-> (exec-post base-uri "" valid-create-firewall)
        (t/is-status 403)))

  (testing "When authenticated, adding a valid representation should succeed"
    (-> (exec-post base-uri "joe" valid-create-firewall)
        (t/is-status 201)))

  (testing "Adding an invalid representation should fail"
    (-> (exec-post base-uri "john" invalid-create-firewall)
        (t/is-status 400)))
    (-> (exec-post base-uri "" invalid-create-firewall)
        (t/is-status 400)))

(deftest query-collection
  (exec-post base-uri "oliver" valid-create-firewall)

  (-> (exec-request base-uri "" "")
      (t/is-status 403))

  (-> (exec-request base-uri "" "jack")
      (t/is-status 200)
      (t/is-key-value :count 0))

  ;; although oliver added the resource, the :acl in it does not grant him access
  (-> (exec-request base-uri "" "oliver")
      (t/is-status 200)
      (t/is-key-value :count 0))

  (-> (exec-request base-uri "" "john")
      (t/is-status 200)
      (t/is-key-value :count 1))

  (-> (exec-request base-uri "" "oliver ADMIN")
      (t/is-status 200)
      (t/is-key-value :count 1)))

(defn- uuid-inserted-for-john!
  []
  (-> (exec-post base-uri "john" valid-create-firewall)
      (get-in [:response :body :resource-id])
      (superstring.core/split #"/")
      second))

(deftest find-collection
  (let [uuid (uuid-inserted-for-john!)]

    (testing "Retrieving the element is forbidden for jack"
      (-> (exec-request (str base-uri "/" uuid) "" "jack")
          (t/is-status 403)))

    (testing "john is able to retrieve element with correct UUID"
      (-> (exec-request (str base-uri "/" uuid) "" "john")
          (t/is-status 200)
          (get-in [:response :body])
          (dissoc :id :created :updated :operations)
          (= valid-create-firewall)
          is))

    (testing "Retrieving element with wrong uuid return 404 for everyone"
      (-> (exec-request (str base-uri "/123123") "" "jack")
          (t/is-status 404))

      (-> (exec-request (str base-uri "/123123") "" "john")
          (t/is-status 404)))))

(deftest delete-collection
  (let [uuid (uuid-inserted-for-john!)]

    (testing "Deleting the element with incorrect uuid returns 404"
      (-> (exec-request (str base-uri "/666" ) "" "jack" :delete "")
          (t/is-status 404))
      (-> (exec-request (str base-uri "/666" ) "" "john" :delete "")
          (t/is-status 404)))

    (testing "Deleting the element with correct uuid is forbidden for jack"
      (-> (exec-request (str base-uri "/" uuid) "" "jack" :delete "")
          (t/is-status 403)))

    (testing "Deleting the element with correct uuid is allowed for john"
      (-> (exec-request (str base-uri "/" uuid) "" "john")
          (t/is-status 200))
      (-> (exec-request (str base-uri "/" uuid) "" "john" :delete "")
          (t/is-status 204))
      (-> (exec-request (str base-uri "/" uuid) "" "john")
          (t/is-status 404)))))

(deftest update-collection
  (let [uuid (uuid-inserted-for-john!)]

    (testing "Updating the element with correct uuid is forbidden for jack"
      (-> (exec-request (str base-uri "/" uuid) "" "jack" :put "")
          (t/is-status 403)))

    (testing "Updating the element with correct uuid is allowed for john"
      (-> (exec-request (str base-uri "/" uuid) "" "john")
          (t/is-key-value :state "STARTED"))

      (-> (exec-request (str base-uri "/" uuid) "" "john" :put updated-firewall)
          (t/is-status 200))

      (-> (exec-request (str base-uri "/" uuid) "" "john")
          (t/is-key-value :state "STOPPED")))

    (testing "Updating with partial representation is allowed for john"
      (-> (exec-request (str base-uri "/" uuid) "" "john" :put partial-firewall)
          (t/is-status 200))

      (-> (exec-request (str base-uri "/" uuid) "" "john")
          (t/is-key-value :state "ERROR")
          (t/has-key :created)))

    (testing "Updating the element with invalid representation is forbidden for john"
      (-> (exec-request (str base-uri "/" uuid) "" "john" :put invalid-create-firewall)
          (t/is-status 400)))))
