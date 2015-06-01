(ns com.sixsq.slipstream.ssclj.resources.event-test
  (:require
    [clojure.test :refer :all]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [korma.core :as kc]

    [peridot.core :refer :all]

    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]

    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.api.acl :as acl]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]
    [com.sixsq.slipstream.ssclj.resources.event :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(defn reset-events
  []
  (acl/-init)
  (dbdb/init-db)
  (kc/delete dbdb/resources))

(defn reset-events-fixtures
  [f]
  (reset-events)
  (f))

(use-fixtures :each reset-events-fixtures)

(def base-uri (str c/service-context resource-name))

(defn make-ring-app [resource-routes]
  (db/set-impl! (dbdb/get-instance))
  (-> resource-routes
      wrap-exceptions
      wrap-base-uri
      wrap-cimi-params
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

(defn insert-some-events
  []
  (reset-events)
  (dotimes [i 20]
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "jane")
        (request base-uri
                 :request-method :post
                 :body (json/write-str (assoc-in valid-event [:content :resource :href] (str "Run/" i))))
        (t/body->json)
        (t/is-status 201)
        (t/location))))

(defn is-count
  [expected-count query-string]
  (insert-some-events)
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request (str base-uri query-string)
               :content-type "application/x-www-form-urlencoded")
      (t/body->json)
      (t/is-key-value :count expected-count)))

(deftest resources-pagination
  (is-count 20  "")
  (is-count 0   "?$first=10&$last=5")
  (is-count 6   "?$first=15")
  (is-count 12  "?$last=12")
  (is-count 2   "?$first=18&$last=19"))

(deftest resources-filtering
  (doseq [i (range 20)]
    (is-count 1 (str "?$filter=content/resource/href='Run/" i "'")))
  (is-count 0 "?$filter=content/resource/href='Run/100'")

  ; content/resource/href='Run/3' and type='state'
  (is-count 1 "?%24filter=content%2Fresource%2Fhref%3D%27Run%2F3%27%20and%20type%3D%27state%27")

  ; content/resource/href='Run/3' and type='WRONG'
  (is-count 0 "?%24filter=content%2Fresource%2Fhref%3D%27Run%2F3%27%20and%20type%3D%27WRONG%27")
  (is-count 20 "?$filter=type='state'"))

(deftest filter-or
  (is-count 20 "?%24filter=%28type%3D%27state%27%29%20or%20%28type%3D%27XXX%27%29")
  (is-count 20 "?%24filter=%28type%3D%27XXXX%27%29%20or%20%28type%3D%27state%27%29"))
