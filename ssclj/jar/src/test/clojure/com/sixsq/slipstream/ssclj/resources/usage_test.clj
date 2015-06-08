(ns com.sixsq.slipstream.ssclj.resources.usage-test
  (:require
    [clojure.test                                               :refer :all]
    [ring.middleware.json                                       :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params                                     :refer [wrap-params]]
    [clj-time.core                                              :as time]
    [korma.core                                                 :as kc]

    [peridot.core                                               :refer :all]

    [com.sixsq.slipstream.ssclj.resources.common.debug-utils    :as du]  
    [com.sixsq.slipstream.ssclj.resources.common.schema         :as c]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params          :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header    :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri             :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.usage-params          :refer [wrap-usage-params]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler    :refer [wrap-exceptions]]

    [com.sixsq.slipstream.ssclj.api.acl                         :as acl]
    [com.sixsq.slipstream.ssclj.usage.record-keeper             :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils                     :as u]
    [com.sixsq.slipstream.ssclj.resources.usage                 :refer :all]

    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils  :as t]
    [com.sixsq.slipstream.ssclj.resources.test-utils            :refer [ring-app *base-uri* *auth-name* exec-request]]
    ))

(defn reset-summaries
  [f]
  (acl/-init)
  (kc/delete rc/usage_summaries)
  (kc/delete acl/acl)
  (f))

(use-fixtures :each reset-summaries)

(def base-uri (str c/service-context resource-name))
(alter-var-root #'*base-uri*  (constantly base-uri))
(alter-var-root #'*auth-name* (constantly "joe"))


(defn daily-summary
  "convenience function"
  [user cloud [year month day] usage]
  { :user                user
    :cloud               cloud
    :start_timestamp     (u/timestamp year month day)
    :end_timestamp       (u/timestamp-next-day year month day)
    :usage               usage })

(defn every-timestamps?
  [pred? ts]
  (every? (fn [[a b]] (pred? (u/to-time a) (u/to-time b))) (partition 2 1 ts)))

(defn are-desc-dates?
  [m]
  (->> (get-in m [:response :body :usages])
       (map :end_timestamp)
       (every-timestamps? (complement time/before?))
       is)
  m)

(defn are-all-usages?
  [m field expected]
  (->> (get-in m [:response :body :usages])
       (map field)
       distinct
       (= [expected])
       is)
  m)

(defn insert-summaries
  []
  (rc/insert-summary! (daily-summary "joe" "exo"    [2015 04 16] {:ram { :unit_minutes 100.0}}))
  (rc/insert-summary! (daily-summary "joe" "exo"    [2015 04 17] {:ram { :unit_minutes 200.0}}))
  (rc/insert-summary! (daily-summary "mike" "aws"   [2015 04 18] {:ram { :unit_minutes 500.0}}))
  (rc/insert-summary! (daily-summary "mike" "exo"   [2015 04 16] {:ram { :unit_minutes 300.0}}))
  (rc/insert-summary! (daily-summary "mike" "aws"   [2015 04 17] {:ram { :unit_minutes 400.0}})))

(deftest get-should-return-most-recent-first-by-user
  (insert-summaries)
  (-> (exec-request "" "joe")
      (t/is-key-value :count 2)
      are-desc-dates?
      (are-all-usages? :user "joe"))

  (-> (exec-request "" "mike")
      (t/is-key-value :count 3)
      are-desc-dates?
      (are-all-usages? :user "mike")))

(deftest acl-filter-cloud-with-role
  (insert-summaries)
  (-> (exec-request "" "john exo1 exo")
      (t/is-key-value :count 3)
      are-desc-dates?
      (are-all-usages? :cloud "exo")))

(defn last-uuid
  []
  (let [full-uuid (-> (kc/select rc/usage_summaries (kc/limit 1))
                      first
                      :id)
        uuid (-> full-uuid
                 (clojure.string/split #"/")
                 second)]
    [uuid full-uuid]))

(deftest get-uuid-with-correct-authn
  (insert-summaries)
  (let [[uuid full-uuid] (last-uuid)]
    (-> (exec-request (str "/" uuid) "john exo")
        (t/is-key-value :id full-uuid)
        (t/is-status 200))))

(deftest get-uuid-without-correct-authn
  (insert-summaries)
  (let [[uuid _] (last-uuid)]
    (-> (exec-request (str "/" uuid) "jack")
        (t/is-status 403))))

(deftest pagination-full
  (insert-summaries)
  (-> (exec-request "?$first=0&$last=10" "mike")
      (t/is-status 200)
      (t/is-key-value :count 3)))

(deftest pagination-only-one
  (insert-summaries)
  (-> (exec-request "?$first=1&$last=1" "mike")
      (t/is-status 200)
      (t/is-key-value :count 1)))

(deftest pagination-outside-bounds
  (insert-summaries)
  (-> (exec-request "?$first=10&$last=15" "mike")
      (t/is-status 200)
      (t/is-key-value :count 0)))

(deftest pagination-first-larger-than-last
  (insert-summaries)
  (-> (exec-request "?$first=10&$last=5" "mike")
      (t/is-key-value :count 0)))

(defn expect-pagination
  [code query-strings]
  (doseq [query-string query-strings]
    (-> (exec-request query-string "mike")
        (t/is-status code))))

(deftest pagination-wrong-query-ignores-invalid
  (insert-summaries)
  (expect-pagination 200
      ["?$first=a&$last=10"])
  (expect-pagination 200
      ["?$first=-1&$last=10"
      "?$first=1&$last=-10"
      "?$first=-1&$last=-10"]))

(deftest pagination-does-not-check-max-limit
  (insert-summaries)
  (expect-pagination 200
    [ "?$first=1&$last=1000000"]))

(deftest from-to
  (insert-summaries)
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "joe")
      (request (str base-uri "?from=2015-04-17&duration=day"))
      t/body->json))
;; TODO
      ;(t/is-status 200)
      ;(t/is-key-value :count 1)))
