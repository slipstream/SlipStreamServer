(ns com.sixsq.slipstream.ssclj.resources.usage-test
  (:require
    [clojure.test                                               :refer :all]

    [clj-time.core                                              :as time]
    [korma.core                                                 :as kc]

    [peridot.core                                               :refer :all]


    [com.sixsq.slipstream.ssclj.resources.common.debug-utils    :as du]  
    [com.sixsq.slipstream.ssclj.resources.common.schema         :as c]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header    :refer [authn-info-header]]

    [com.sixsq.slipstream.ssclj.api.acl                         :as acl]
    [com.sixsq.slipstream.ssclj.usage.record-keeper             :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils                     :as u]
    [com.sixsq.slipstream.ssclj.resources.usage                 :refer :all]
    [com.sixsq.slipstream.ssclj.app.params                      :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils  :as t]
    [com.sixsq.slipstream.ssclj.resources.test-utils            :refer [ring-app *base-uri* *auth-name*
                                                                        exec-request is-count]]
    ))

(defn daily-summary
  "convenience function"
  [user cloud [year month day] usage]
  { :user                user
   :cloud               cloud
   :start_timestamp     (u/timestamp year month day)
   :end_timestamp       (u/timestamp-next-day year month day)
   :usage               usage })


(defn insert-summaries
  [f]
  (acl/-init)
  (rc/-init)
  (kc/delete rc/usage_summaries)
  (kc/delete acl/acl)
  (rc/insert-summary! (daily-summary "joe" "exo"    [2015 04 16] {:ram { :unit_minutes 100.0}}))
  (rc/insert-summary! (daily-summary "joe" "exo"    [2015 04 17] {:ram { :unit_minutes 200.0}}))
  (rc/insert-summary! (daily-summary "mike" "aws"   [2015 04 18] {:ram { :unit_minutes 500.0}}))
  (rc/insert-summary! (daily-summary "mike" "exo"   [2015 04 16] {:ram { :unit_minutes 300.0}}))
  (rc/insert-summary! (daily-summary "mike" "aws"   [2015 04 17] {:ram { :unit_minutes 40.0}}))
  (f))

(use-fixtures :once insert-summaries)

(def base-uri (str p/service-context resource-name))
(alter-var-root #'*base-uri*  (constantly base-uri))
(alter-var-root #'*auth-name* (constantly "joe"))


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

(deftest get-should-return-most-recent-first-by-user
  (-> (exec-request "" "joe")
      (t/is-key-value :count 2)
      are-desc-dates?
      (are-all-usages? :user "joe"))

  (-> (exec-request "" "mike")
      (t/is-key-value :count 3)
      are-desc-dates?
      (are-all-usages? :user "mike")))

(deftest acl-filter-cloud-with-role
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
  (let [[uuid full-uuid] (last-uuid)]
    (-> (exec-request (str base-uri "/" uuid) ""  "john exo")
        (t/is-key-value :id full-uuid)
        (t/is-status 200))))

(deftest get-uuid-without-correct-authn
  (let [[uuid _] (last-uuid)]
    (-> (exec-request (str base-uri "/" uuid) ""  "intruder")
        (t/is-status 403))))

(deftest pagination-full
  (-> (exec-request "?$first=0&$last=10" "mike")
      (t/is-status 200)
      (t/is-key-value :count 3)))

(deftest pagination-only-one
  (-> (exec-request "?$first=1&$last=1" "mike")
      (t/is-status 200)
      (t/is-key-value :count 1)))

(deftest pagination-outside-bounds
  (-> (exec-request "?$first=10&$last=15" "mike")
      (t/is-status 200)
      (t/is-key-value :count 0)))

(deftest pagination-first-larger-than-last
  (-> (exec-request "?$first=10&$last=5" "mike")
      (t/is-key-value :count 0)))

(defn expect-pagination
  [code query-strings]
  (doseq [query-string query-strings]
    (-> (exec-request query-string "mike")
        (t/is-status code))))

(deftest pagination-wrong-query-ignores-invalid
  (expect-pagination 200
      ["?$first=a&$last=10"])
  (expect-pagination 200
      ["?$first=-1&$last=10"
      "?$first=1&$last=-10"
      "?$first=-1&$last=-10"]))

(deftest pagination-does-not-check-max-limit
  (expect-pagination 200
    [ "?$first=1&$last=1000000"]))


(deftest admin-sees-everything
  (is-count 5 "" "super ADMIN"))

(deftest simple-filter-with-admin
  (is-count 2 "?$filter=user='joe'"   "super ADMIN")
  (is-count 3 "?$filter=user='mike'"  "super ADMIN")
  )

(deftest filter-int-value
  (is-count 1 "?$filter=usage/ram/unit_minutes < 100" "super ADMIN")
  (is-count 1 "?$filter=usage/ram/unit_minutes > 400" "super ADMIN")
  (is-count 1 "?$filter=usage/ram/unit_minutes < 50" "super ADMIN")
  (is-count 1 "?$filter=usage/ram/unit_minutes < 50 and usage/ram/unit_minutes > 30" "super ADMIN")
  (is-count 2 "?$filter=usage/ram/unit_minutes > 100 and usage/ram/unit_minutes < 500" "super ADMIN")

  (is-count 1 "?$filter=usage/ram/unit_minutes = 40" "super ADMIN")
  (is-count 1 "?$filter=usage/ram/unit_minutes = 100" "super ADMIN")
  (is-count 1 "?$filter=usage/ram/unit_minutes = 200" "super ADMIN")
  (is-count 1 "?$filter=usage/ram/unit_minutes = 300" "super ADMIN")
  (is-count 1 "?$filter=usage/ram/unit_minutes = 500" "super ADMIN"))

(deftest filter-with-cimi-filter-unknown-to-db
  (is-count 2 "?$filter=user='joe'" "super ADMIN")
  ;; usage/ram/unit will *not* be filtered at sql level
  (is-count 1 "?$filter=usage/ram/unit_minutes='100.0'" "super ADMIN"))

(defn- one-line
  [s]
  (clojure.string/replace s #"\n" ""))

(deftest filter-with-admin
  (is-count 2 (one-line
                "?$filter=
                 start_timestamp='2015-04-16T00:00:00.000Z'
                 and
                 end_timestamp='2015-04-17T00:00:00.000Z'") "super ADMIN")

  (is-count 1 (one-line
                "?$filter=
                 user='joe'
                 and
                 start_timestamp='2015-04-16T00:00:00.000Z'
                 and
                 end_timestamp='2015-04-17T00:00:00.000Z'") "super ADMIN")

  (is-count 2 (one-line
                "?$filter=
                 user='joe'") "super ADMIN")

  (is-count 3 (one-line
                "?$filter=
                 user='mike'") "super ADMIN")

  (is-count 1 (one-line
                "?$filter=
                 user='joe'
                 and
                 start_timestamp='2015-04-17T00:00:00.000Z'
                 and
                 end_timestamp='2015-04-18T00:00:00.000Z'") "super ADMIN")

  (is-count 0 (one-line
              "?$filter=
               user='joe'
               and
               start_timestamp='2015-04-18T00:00:00.000Z'
               and
               end_timestamp='2015-04-19T00:00:00.000Z'") "super ADMIN")

  (is-count 1 (one-line
              "?$filter=
               user='mike'
               and
               start_timestamp='2015-04-18T00:00:00.000Z'
               and
               end_timestamp='2015-04-19T00:00:00.000Z'") "super ADMIN"))
