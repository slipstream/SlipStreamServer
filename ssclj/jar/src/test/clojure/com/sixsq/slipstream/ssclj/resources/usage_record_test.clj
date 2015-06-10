(ns com.sixsq.slipstream.ssclj.resources.usage-record-test
  (:require
    [clojure.test                                               :refer :all]
    [korma.core                                                 :as kc]

    [peridot.core                                               :refer :all]
    [ring.middleware.json                                       :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params                                     :refer [wrap-params]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header    :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri             :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler    :refer [wrap-exceptions]]

    [com.sixsq.slipstream.ssclj.api.acl                         :as acl]
    [com.sixsq.slipstream.ssclj.db.impl                         :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding             :as dbdb]
    [com.sixsq.slipstream.ssclj.usage.record-keeper             :as rc]
    [com.sixsq.slipstream.ssclj.resources.usage-record          :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes                      :as routes]
    [com.sixsq.slipstream.ssclj.app.params                      :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils  :as t]
    [clojure.data.json                                          :as json]))

(defn reset-records
  [f]
  (rc/-init)
  (kc/delete rc/usage_records)
  (kc/delete acl/acl)
  (f))

(use-fixtures :each reset-records)

(def base-uri (str p/service-context resource-name))

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

(deftest get-without-authn-succeeds
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri)
      t/body->json
      (t/is-status 200)))

(def valid-usage-record
  { :acl {
          :owner {:type "USER" :principal "joe"}
          :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}
    :id                     "UsageRecord/ec39566e-87b1-4ec9-b994-31523420028b"
    :resourceURI            resource-uri
    :cloud_vm_instanceid    "exoscale-ch-gva:9010d739-6933-4652-9db1-7bdafcac01cb"
    :user                   "joe"
    :cloud                  "aws"
    :start_timestamp        "2015-05-04T15:32:22.853Z"
    :metrics                [{ :name "vm"
                              :value "1.0"}]})

(def invalid-usage-record
  (dissoc valid-usage-record :acl))

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
               :body (json/write-str invalid-usage-record))
      t/body->json
      (t/is-status 400)))
