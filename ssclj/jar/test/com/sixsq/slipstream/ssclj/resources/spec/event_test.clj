(ns com.sixsq.slipstream.ssclj.resources.spec.event-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.event :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]))

(def event-timestamp "2015-01-16T08:05:00.0Z")

(def valid-event {
                  :acl         {
                                :owner {
                                        :type "USER" :principal "joe"}
                                :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}

                  :id          "event/262626262626262"

                  :resourceURI resource-uri

                  :timestamp   event-timestamp
                  :content     {
                                :resource {:href "module/HNSciCloud-RHEA/S3"}
                                :state    "Started"}
                  :type        "state"
                  :severity    "critical"
                  })

(defn valid? [event] (s/valid? :cimi/event event))
(def invalid? (complement valid?))

(deftest check-event
  (is (= valid-event (crud/validate valid-event)))
  (is (valid? valid-event)))

(deftest check-reference
  (let [updated-event (assoc-in valid-event [:content :resource :href] "another/valid-identifier")]
    (is (= updated-event (crud/validate updated-event)))
    (is (valid? updated-event)))
  (let [updated-event (assoc-in valid-event [:content :resource :href] "/not a valid reference/")]
    (is (invalid? updated-event))))

(deftest check-severity
  (doseq [valid-severity ["critical" "high" "medium" "low"]]
    (is (valid? (assoc valid-event :severity valid-severity))))
  (is (invalid? (assoc valid-event :severity "unknown-severity"))))

(deftest check-type
  (doseq [valid-type ["state" "alarm"]]
    (is (valid? (assoc valid-event :type valid-type))))
  (is (invalid? (assoc valid-event :type "unknown-type"))))
