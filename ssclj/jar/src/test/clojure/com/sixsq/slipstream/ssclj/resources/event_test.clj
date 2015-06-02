(ns com.sixsq.slipstream.ssclj.resources.event-test
  (:require
    [clojure.string                                             :as s]
    [clojure.test                                               :refer :all]
    [ring.middleware.json                                       :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params                                     :refer [wrap-params]]
    [ring.util.codec                                            :as rc]
    [korma.core                                                 :as kc]

    [peridot.core                                               :refer :all]

    [com.sixsq.slipstream.ssclj.resources.common.schema         :as c]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header    :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri             :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params          :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler    :refer [wrap-exceptions]]

    [clojure.data.json                                          :as json]
    [com.sixsq.slipstream.ssclj.api.acl                         :as acl]
    [com.sixsq.slipstream.ssclj.db.impl                         :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding             :as dbdb]
    [com.sixsq.slipstream.ssclj.resources.event                 :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes                      :as routes]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils  :as t]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils    :as du]))

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

(def ^:private nb-events 4)

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
  (dotimes [i nb-events]
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "jane")
        (request base-uri
                 :request-method :post
                 :body (json/write-str (assoc-in valid-event [:content :resource :href] (str "Run/" i)))))))

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
  (is-count nb-events  "")
  (is-count 0   "?$first=10&$last=5")
  (is-count 2   "?$first=3")
  (is-count 2  "?$last=2")
  (is-count 2   "?$first=3&$last=4"))

(defn- urlencode-param
  [p]
  (->>  (re-seq #"([^=]*)=(.*)" p)
        first
        next
        (map rc/url-encode)
        (s/join "=")))

(defn urlencode-params
  [query-string]
  (let [params (subs query-string 1)]
    (->>  (s/split params #"&")
          (map urlencode-param)
          (s/join "&")
          (str "?"))))

(deftest resources-filtering
  (doseq [i (range nb-events)]
    (is-count 1 (str "?$filter=content/resource/href='Run/" i "'")))
  (is-count 0 "?$filter=content/resource/href='Run/100'")

  (is-count 1 (urlencode-params "?$filter=content/resource/href='Run/3' and type='state'"))
  (is-count 1 (urlencode-params "?$filter=type='state' and content/resource/href='Run/3'"))
  (is-count 1 (urlencode-params "?$filter=type='state'       and     content/resource/href='Run/3'"))

  (is-count 1 (urlencode-params "?$filter=content/resource/href='Run/3'"))
  (is-count 0 (urlencode-params "?$filter=type='WRONG' and content/resource/href='Run/3'"))
  (is-count 0 (urlencode-params "?$filter=content/resource/href='Run/3' and type='WRONG'"))
  (is-count nb-events "?$filter=type='state'"))

(deftest filter-and
  (is-count 0 (urlencode-params "?$filter=type='state' and type='XXX'"))
  (is-count 0 (urlencode-params "?$filter=type='YYY' and type='state'"))
  (is-count 0 (urlencode-params "?$filter=(type='state') and (type='XXX')"))
  (is-count 0 (urlencode-params "?$filter=(type='YYY') and (type='state')")))

(deftest filter-or
  (is-count 0 (urlencode-params "?$filter=type='XXX'"))
  (is-count nb-events (urlencode-params "?$filter=type='state'"))
  (is-count nb-events (urlencode-params "?$filter=type='state' or type='XXXX'"))
  (is-count nb-events (urlencode-params "?$filter=type='XXXX' or type='state'"))
  (is-count nb-events (urlencode-params "?$filter=(type='state') or (type='XXX')"))
  (is-count nb-events (urlencode-params "?$filter=(type='XXXXX') or (type='state')"))
  (is-count 0 (urlencode-params "?$filter=type='XXXXX' or type='YYYY'"))
  (is-count 0 (urlencode-params "?$filter=(type='XXXXX') or (type='YYYY')")))

(deftest filter-multiple
  (is-count 0 (urlencode-params "?$filter=type='state'&$filter=type='XXX'"))
  (is-count 1 (urlencode-params "?$filter=type='state'&$filter=content/resource/href='Run/3'")))
