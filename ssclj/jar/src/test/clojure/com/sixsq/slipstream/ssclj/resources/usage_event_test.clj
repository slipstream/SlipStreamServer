(ns com.sixsq.slipstream.ssclj.resources.usage-event-test
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
    [com.sixsq.slipstream.ssclj.resources.usage-event :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]))

(defn reset-events
  [f]
  (rc/-init)
  (dbdb/init-db)
  (kc/delete dbdb/resources)
  (kc/delete acl/acl)
  (f))

(use-fixtures :each reset-events)

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

(def valid-usage-event
  {:acl                 {
                         :owner {:type "USER" :principal "joe"}
                         :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}
   :cloud-vm-instanceid "exoscale-ch-gva:9010d739-6933-4652-9db1-7bdafcac01cb"
   :user                "joe"
   :cloud               "aws"
   :start-timestamp     "2015-05-04T15:32:22.853Z"
   :metrics             [{:name  "vm"
                          :value "1.0"}]})

(deftest test-post-usage-event
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "joe")
      (request "/api/usage-event"
               :request-method :post
               :body (json/write-str valid-usage-event))
      t/body->json
      (t/is-status 201)))
