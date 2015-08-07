(ns com.sixsq.slipstream.ssclj.resources.event-test
  (:require
    [clojure.test :refer :all]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [korma.core :as kc]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.api.acl :as acl]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.event :refer :all]

    [com.sixsq.slipstream.ssclj.app.params :as p]

    [com.sixsq.slipstream.ssclj.resources.test-utils :as tu :refer [ring-app urlencode-params is-count exec-request]]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))


(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(def ^:private nb-events 10)

(def valid-event {
                  :acl {
                        :owner {
                                :type "USER" :principal "jane"}
                               :rules [{:type "USER" :principal "jane" :right "ALL"}]}
                       :timestamp "2015-01-16T08:05:00.0Z"
                  :content  {
                             :resource {:href "run/45614147-aed1-4a24-889d-6365b0b1f2cd"}
                                       :state "Started"}
                  :type "state"
                  :severity "critical"
                  })

(def valid-events
  (for [i (range nb-events)]
    (-> valid-event
        (assoc-in [:content :resource :href] (str "run/" i))
        (assoc :timestamp (if (even? i) "2016-01-16T08:05:00.0Z" "2015-01-16T08:05:00.0Z")))))

(defn insert-some-events
  []
  (db/set-impl! (dbdb/get-instance))
  (dbdb/init-db)
  (kc/delete dbdb/resources)
  (kc/delete acl/acl)
  (let [state (-> (session (ring-app))
                  (content-type "application/json")
                  (header authn-info-header "jane"))]
    (doseq [valid-event valid-events]
      (request state base-uri
               :request-method :post
               :body (json/write-str valid-event)))))

(defn fixture-insert-some-events
  [f]
  (insert-some-events)
  (f))

(use-fixtures :once fixture-insert-some-events)

;;
;; Note that these tests need nb-events > 5
;;

(defn event-is-count
  [expected-count query-string]
  (is-count base-uri expected-count query-string "jane"))

(deftest events-are-retrieved-most-recent-first
  (->> valid-events
       (map :timestamp)
       tu/ordered-desc?
       false?
       is)

  (->>  (-> (exec-request base-uri "" "jane")
            (get-in [:response :body :events]))
        (map :timestamp)
        tu/ordered-desc?
        is))

(defn timestamp-paginate-single
  [n]
  (-> (exec-request base-uri (str "?$first=" n "&$last=" n) "jane")
      (get-in [:response :body :events])
      first
      :timestamp))

;; Here, timestamps are retrieved one by one (due to pagination)
(deftest events-are-retrieved-most-recent-first-when-paginated
  (-> (map timestamp-paginate-single (range 1 (inc nb-events)))
      tu/ordered-desc?
      is))

(deftest resources-pagination
  (event-is-count nb-events  "")
  (event-is-count 0   "?$first=10&$last=5")

  (event-is-count (- nb-events 2)   "?$first=3")
  (event-is-count 2  "?$last=2")
  (event-is-count 2  "?$first=3&$last=4"))

(deftest pagination-occurs-after-filtering
  (event-is-count 1 "?$filter=content/resource/href='run/5'")
  (event-is-count 1 "?$filter=content/resource/href='run/5'&$last=1")
  (event-is-count 1 "?$last=1&$filter=content/resource/href='run/5'"))

(deftest resources-filtering
  (doseq [i (range nb-events)]
    (event-is-count 1 (str "?$filter=content/resource/href='run/" i "'")))
  (event-is-count 0 "?$filter=content/resource/href='run/100'")

  (event-is-count 1 "?$filter=content/resource/href='run/3' and type='state'")
  (event-is-count 1 "?$filter=type='state' and content/resource/href='run/3'")
  (event-is-count 1 "?$filter=type='state'       and     content/resource/href='run/3'")

  (event-is-count 1 "?$filter=content/resource/href='run/3'")
  (event-is-count 0 "?$filter=type='WRONG' and content/resource/href='run/3'")
  (event-is-count 0 "?$filter=content/resource/href='run/3' and type='WRONG'")
  (event-is-count nb-events "?$filter=type='state'"))

(deftest filter-and
  (event-is-count nb-events "$filter=type='state' and timestamp='2015-01-16T08:05:00.0Z'")
  (event-is-count 0 "?$filter=type='state' and type='XXX'")
  (event-is-count 0 "?$filter=type='YYY' and type='state'")
  (event-is-count 0 "?$filter=(type='state') and (type='XXX')")
  (event-is-count 0 "?$filter=(type='YYY') and (type='state')"))

(deftest filter-or
  (event-is-count 0 "?$filter=type='XXX'")
  (event-is-count nb-events "?$filter=type='state'")
  (event-is-count nb-events "?$filter=type='state' or type='XXXX'")
  (event-is-count nb-events "?$filter=type='XXXX' or type='state'")
  (event-is-count nb-events "?$filter=(type='state') or (type='XXX')")
  (event-is-count nb-events "?$filter=(type='XXXXX') or (type='state')")
  (event-is-count 0 "?$filter=type='XXXXX' or type='YYYY'")
  (event-is-count 0 "?$filter=(type='XXXXX') or (type='YYYY')"))

(deftest filter-multiple
  (event-is-count 0 "?$filter=type='state'&$filter=type='XXX'")
  (event-is-count 1 "?$filter=type='state'&$filter=content/resource/href='run/3'"))
