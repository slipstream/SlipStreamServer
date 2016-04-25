(ns com.sixsq.slipstream.ssclj.resources.usage-record-test
  (:require
    [clojure.test :refer :all]
    [korma.core :as kc]

    [peridot.core :refer :all]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]

    [com.sixsq.slipstream.ssclj.api.acl :as acl]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.resources.usage-record :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [com.sixsq.slipstream.ssclj.resources.test-utils :as tu]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(defn reset-records
  [f]
  (rc/-init)
  (dbdb/init-db)
  (kc/delete dbdb/resources)
  (kc/delete acl/acl)
  (f))

(use-fixtures :each reset-records)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn make-ring-app [resource-routes]
  (db/set-impl! (dbdb/get-instance))
  (-> resource-routes
      wrap-exceptions
      wrap-base-uri
      wrap-params
      wrap-authn-info-header
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})))

(defn ring-app []
  (make-ring-app (t/concat-routes routes/final-routes)))

(def valid-usage-record
  {:acl                 {:owner {:type "USER" :principal "joe"}
                         :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}
   :cloud-vm-instanceid "exoscale-ch-gva:9010d739-6933-4652-9db1-7bdafcac01cb"
   :user                "joe"
   :cloud               "aws"
   :start-timestamp     "2015-05-04T15:32:22.853Z"
   :metric-name         "vm"
   :metric-value        "1.0"})

(def invalid-usage-record
  (dissoc valid-usage-record :user))

(def closed-usage-record
  (assoc valid-usage-record :end-timestamp  "2015-05-04T15:40:15.432Z"))

(deftest get-without-authn-succeeds
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri)
      t/body->json
      (t/is-status 200)))

(deftest only-snake-url-succeeds
  (-> (session (ring-app))
      (content-type "application/json")
      (request (str p/service-context resource-name))
      t/body->json
      (t/is-status 405)))

(deftest test-post-when-authenticated
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "joe")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-usage-record))
      t/body->json
      (t/is-status 201)))

(deftest test-post-when-NOT-authenticated
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-usage-record))
      t/body->json
      (t/is-status 201)))

(deftest test-post-invalid-record
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri
               :request-method :post
               :body (json/write-str invalid-usage-record))
      t/body->json
      (t/is-status 400)))

(deftest test-edit
  (let [id (-> (session (ring-app))
               (content-type "application/json")
               (header authn-info-header "joe")
               (request base-uri
                        :request-method :post
                        :body (json/write-str valid-usage-record))
               t/body->json
               (get-in [:response :body :resource-id]))
        uri (str p/service-context id)]

    ; edit existing usage-record
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "joe")
        (request uri
                 :request-method :put
                 :body (json/write-str closed-usage-record))
        t/body->json
        (t/is-status 200))

    ; get modified usage-record
    (let [urs (-> (session (ring-app))
                  (content-type "application/json")
                  (header authn-info-header "joe")
                  (request base-uri :request-method :get)
                  t/body->json
                  (t/is-key-value :count 1)
                  (get-in [:response :body :usage-records]))]
      (is (= 1 (count urs)))
      (is (tu/submap? closed-usage-record (first urs))))

    ; try edit non-existing usage-record
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "joe")
        (request (str p/service-context "wrong-id")
                 :request-method :put
                 :body (json/write-str closed-usage-record))
        t/body->json
        (t/is-status 405))))

