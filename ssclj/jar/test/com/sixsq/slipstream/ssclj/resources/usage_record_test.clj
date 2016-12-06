(ns com.sixsq.slipstream.ssclj.resources.usage-record-test
  (:require
    [clojure.test :refer :all]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.usage-record :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.test-utils :as tu]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.usage-record :as ur]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.db.es.es-util :as esu]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn make-ring-app [resource-routes]
  (db/set-impl! (esb/get-instance))
  (-> resource-routes
      wrap-exceptions
      wrap-base-uri
      wrap-params
      wrap-authn-info-header
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})))

(defn ring-app []
  (make-ring-app (ltu/concat-routes routes/final-routes)))

(def valid-usage-record
  {:acl                 {:owner {:type "USER" :principal "joe"}
                         :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}
   :cloud-vm-instanceid "exoscale-ch-gva:9010d739-6933-4652-9db1-7bdafcac01cb"
   :user                "joe"
   :cloud               "aws"
   :start-timestamp     "2015-05-04T15:32:22.853Z"
   :end-timestamp       ur/date-in-future
   :metric-name         "vm"
   :metric-value        "1.0"})

(def invalid-usage-record
  (assoc valid-usage-record :end_timestamp "2015-05-04T18:07:15.456Z"))

(defn build-usage-record
  [cloud-vm-instance-id start-timestamp metric-name metric-value]
  (assoc valid-usage-record
    :cloud-vm-instanceid cloud-vm-instance-id
    :start-timestamp start-timestamp
    :metric-name metric-name
    :metric-value metric-value))

(defn build-close-usage-record
  [cloud-vm-instance-id start-timestamp metric-name metric-value end-timestamp]
  (assoc (build-usage-record cloud-vm-instance-id start-timestamp metric-name metric-value) :end-timestamp end-timestamp))

(def closed-usage-record
  (assoc valid-usage-record :end-timestamp  "2015-05-04T15:40:15.432Z"))

(deftest get-without-authn-succeeds
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri)
      ltu/body->edn
      (ltu/is-status 200)))

(deftest only-snake-url-succeeds
  (-> (session (ring-app))
      (content-type "application/json")
      (request (str p/service-context resource-name))
      ltu/body->edn
      (ltu/is-status 405)))

(deftest test-post-when-authenticated
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "joe")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-usage-record))
      ltu/body->edn
      (ltu/is-status 201)))

(deftest test-post-when-NOT-authenticated
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-usage-record))
      ltu/body->edn
      (ltu/is-status 201)))

(deftest test-post-invalid-record
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri
               :request-method :post
               :body (json/write-str invalid-usage-record))
      ltu/body->edn
      (ltu/is-status 400)))

(deftest test-edit
  (let [id (-> (session (ring-app))
               (content-type "application/json")
               (header authn-info-header "joe")
               (request base-uri
                        :request-method :post
                        :body (json/write-str valid-usage-record))
               ltu/body->edn
               (get-in [:response :body :resource-id]))
        uri (str p/service-context id)]

    ; edit existing usage-record
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "joe")
        (request uri
                 :request-method :put
                 :body (json/write-str closed-usage-record))
        ltu/body->edn
        (ltu/is-status 200)
        (ltu/is-key-value :end-timestamp "2015-05-04T15:40:15.432Z")))

  ; get modified usage-record
  (let [urs (-> (session (ring-app))
                (content-type "application/json")
                (header authn-info-header "joe ADMIN")
                (request base-uri :request-method :get)
                ltu/body->edn
                (ltu/is-key-value :count 1)
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
      ltu/body->edn
      (ltu/is-status 405)))

(deftest test-last-record
  (let [state (-> (session (ring-app))
                  (content-type "application/json")
                  (header authn-info-header "joe"))
        ur1   (build-usage-record "cloud1" "2015-05-01T00:00:00.000Z" "disk" "1150.0")
        ur2   (build-usage-record "cloud1" "2015-05-02T00:00:00.000Z" "vm" "1.0")
        ur3   (build-usage-record "cloud1" "2015-05-03T00:00:00.000Z" "vm" "2.0")
        ur31  (build-usage-record "cloud1" "2015-05-10T00:00:00.000Z" "disk" "2.0")
        ur4   (build-usage-record "cloud2" "2015-05-04T00:00:00.000Z" "vm" "3.0")
        ur5   (build-usage-record "cloud1" "2015-05-05T00:00:00.000Z" "vm" "4.0")]
    (request state base-uri :request-method :post :body (json/write-str ur1))
    (request state base-uri :request-method :post :body (json/write-str ur3))
    (request state base-uri :request-method :post :body (json/write-str ur2))
    (request state base-uri :request-method :post :body (json/write-str ur31))

    ;(println "DUMP")
    ;(clojure.pprint/pprint
    ;  (esu/dump esb/client esb/index "usage-record"))
    ;(println (apply str (repeat 20 "-")))

    ;(clojure.pprint/pprint (last-record ur5))

    (is (tu/submap? (dissoc ur3 :acl) (dissoc (last-record ur5) :acl)))
    (is (nil? (last-record ur4)))
    ))


(deftest test-records-for-interval
  (let [state (-> (session (ring-app))
                  (content-type "application/json")
                  (header authn-info-header "joe"))
        ur1 (build-close-usage-record "cloud:1" "2015-05-01T00:00:00.000Z" "disk" "1150.0" "2015-05-01T04:00:00.000Z")
        ur2 (build-usage-record "cloud:1" "2015-05-01T00:00:00.000Z" "disk" "1150.0")
        ]
    (request state base-uri :request-method :post :body (json/write-str ur1))
    (request state base-uri :request-method :post :body (json/write-str ur2))

    (is (thrown? IllegalArgumentException (ur/records-for-interval "2015-06-01T00:00:00.000Z" "2015-04-29T00:00:00.000Z")))
    (is (empty? (ur/records-for-interval "2015-04-01T00:00:00.000Z" "2015-04-29T00:00:00.000Z")))
    (is (= 1 (count (ur/records-for-interval "2015-06-01T00:00:00.000Z" "2015-06-10T00:00:00.000Z"))))
    (is (rc/end-in-future? (first (ur/records-for-interval "2015-06-01T00:00:00.000Z" "2015-06-10T00:00:00.000Z"))))
    (is (= 2 (count (ur/records-for-interval "2015-04-01T00:00:00.000Z" "2015-06-01T00:00:00.000Z"))))
    (is (= 2 (count (ur/records-for-interval "2015-04-01T00:00:00.000Z" "2015-05-01T02:00:00.000Z"))))
    (is (= 2 (count (ur/records-for-interval "2015-05-01T02:00:00.000Z" "2015-05-01T03:00:00.000Z" ))))
    (is (= 2 (count (ur/records-for-interval "2015-05-01T02:00:00.000Z" "2015-06-01T00:00:00.000Z" ))))))

(deftest last-usage-record-when-none
  (is (empty? (ur/last-record valid-usage-record))))
