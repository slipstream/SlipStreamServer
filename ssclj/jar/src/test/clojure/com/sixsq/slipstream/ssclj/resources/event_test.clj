(ns com.sixsq.slipstream.ssclj.resources.event-test
  (:require
    [clojure.test                                               :refer :all]
    [ring.middleware.json                                       :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params                                     :refer [wrap-params]]
    [korma.core                                                 :as kc]

    [peridot.core                                               :refer :all]

    [com.sixsq.slipstream.ssclj.resources.common.schema         :as c]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header    :refer [authn-info-header]]

    [clojure.data.json                                          :as json]
    [com.sixsq.slipstream.ssclj.api.acl                         :as acl]
    [com.sixsq.slipstream.ssclj.db.database-binding             :as dbdb]
    [com.sixsq.slipstream.ssclj.resources.event                 :refer :all]


    [com.sixsq.slipstream.ssclj.resources.test-utils            :refer [urlencode-params is-count *base-uri* *auth-name* ring-app]]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils    :as du]))


(def ^:private nb-events 10)

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

(def base-uri (str c/service-context resource-name))
(alter-var-root #'*base-uri*  (constantly base-uri))
(alter-var-root #'*auth-name* (constantly "jane"))

(defn insert-some-events
  []
  (acl/-init)
  (dbdb/init-db)
  (kc/delete dbdb/resources)
  (kc/delete acl/acl)
  (let [state (-> (session (ring-app))
                  (content-type "application/json")
                  (header authn-info-header *auth-name*))]
    (dotimes [i nb-events]
      (request state base-uri
               :request-method :post
               :body (json/write-str (assoc-in valid-event [:content :resource :href] (str "Run/" i)))))))

(defn fixture-insert-some-events
  [f]
  (insert-some-events)
  (f))

(use-fixtures :once fixture-insert-some-events)

;;
;; Note that these tests need nb-events > 5
;;

(deftest resources-pagination
  (is-count nb-events  "")
  (is-count 0   "?$first=10&$last=5")

  (is-count (- nb-events 2)   "?$first=3")
  (is-count 2  "?$last=2")
  (is-count 2  "?$first=3&$last=4"))

(deftest pagination-occurs-after-filtering
  (is-count 1 "?$filter=content/resource/href='Run/5'")
  (is-count 1 "?$filter=content/resource/href='Run/5'&$last=1")
  (is-count 1 "?$last=1&$filter=content/resource/href='Run/5'"))

(deftest resources-filtering
  (doseq [i (range nb-events)]
    (is-count 1 (str "?$filter=content/resource/href='Run/" i "'")))
  (is-count 0 "?$filter=content/resource/href='Run/100'")

  (is-count 1 "?$filter=content/resource/href='Run/3' and type='state'")
  (is-count 1 "?$filter=type='state' and content/resource/href='Run/3'")
  (is-count 1 "?$filter=type='state'       and     content/resource/href='Run/3'")

  (is-count 1 "?$filter=content/resource/href='Run/3'")
  (is-count 0 "?$filter=type='WRONG' and content/resource/href='Run/3'")
  (is-count 0 "?$filter=content/resource/href='Run/3' and type='WRONG'")
  (is-count nb-events "?$filter=type='state'"))

(deftest filter-and
  (is-count nb-events "$filter=type='state' and timestamp='2015-01-16T08:05:00.0Z'")
  (is-count 0 "?$filter=type='state' and type='XXX'")
  (is-count 0 "?$filter=type='YYY' and type='state'")
  (is-count 0 "?$filter=(type='state') and (type='XXX')")
  (is-count 0 "?$filter=(type='YYY') and (type='state')"))

(deftest filter-or
  (is-count 0 "?$filter=type='XXX'")
  (is-count nb-events "?$filter=type='state'")
  (is-count nb-events "?$filter=type='state' or type='XXXX'")
  (is-count nb-events "?$filter=type='XXXX' or type='state'")
  (is-count nb-events "?$filter=(type='state') or (type='XXX')")
  (is-count nb-events "?$filter=(type='XXXXX') or (type='state')")
  (is-count 0 "?$filter=type='XXXXX' or type='YYYY'")
  (is-count 0 "?$filter=(type='XXXXX') or (type='YYYY')"))

(deftest filter-multiple
  (is-count 0 "?$filter=type='state'&$filter=type='XXX'")
  (is-count 1 "?$filter=type='state'&$filter=content/resource/href='Run/3'"))
