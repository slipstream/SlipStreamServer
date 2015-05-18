(ns com.sixsq.slipstream.ssclj.resources.event-test
  (:require
    [clojure.test                                               :refer :all]
    [ring.middleware.json                                       :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params                                     :refer [wrap-params]]
    [clj-time.core                                              :as time]
    [korma.core                                                 :as kc]

    [peridot.core                                               :refer :all]

    [com.sixsq.slipstream.ssclj.database.korma-helper           :as kh]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils    :as du]
    [com.sixsq.slipstream.ssclj.resources.common.schema         :as c]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header    :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri             :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler    :refer [wrap-exceptions]]

    [clojure.data.json                                          :as json]
    [com.sixsq.slipstream.ssclj.api.acl                         :as acl]
    [com.sixsq.slipstream.ssclj.db.impl                         :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding             :as dbdb]
    [com.sixsq.slipstream.ssclj.usage.record-keeper             :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils                     :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud           :as crud]
    [com.sixsq.slipstream.ssclj.resources.event                 :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes                      :as routes]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils  :as t]))

(defn reset-events
  [f]
  (acl/-init)
  (dbdb/init-db)
  (kc/delete dbdb/resources)
  (f))

(use-fixtures :each reset-events)

(def base-uri (str c/service-context resource-name))

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

(def valid-event { 
  :acl {
    :owner {
      :type "USER" :principal "jane"}
      :rules [{:type "USER" :principal "jane" :right "ALL"}]}
  :timestamp "2015-01-16T08:05:00.0Z"
  :content  {
    :resource {:href "Run/45614147-aed1-4a24-889d-6365b0b1f2cd"}
    :state "Started"} 
  :type "state"
  :severity "critical"
})

(deftest resources-pagination
  ; insert 20 events
  (dotimes [_ 20]
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "jane")
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-event))
        (t/body->json)
        (t/is-status 201)
        (t/location)))
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request base-uri)
      (t/body->json)
      (t/is-key-value :count 20))
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request (str base-uri "?$first=15"))
      (t/body->json)
      (t/is-key-value :count 6))
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request (str base-uri "?$last=12"))
      (t/body->json)
      (t/is-key-value :count 12))
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request (str base-uri "?$first=19&last=20"))
      (t/body->json)
      (t/is-key-value :count 2)))
