(ns com.sixsq.slipstream.ssclj.resources.event-schema-test
  (:require
    [clojure.test :refer [deftest are is]]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.event :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]))

(def event-timestamp "2015-01-16T08:05:00.0Z")

(def valid-event {
                  :acl         {
                                :owner {
                                        :type "USER" :principal "joe"}
                                :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}

                  :id          "Event/262626262626262"

                  :resourceURI resource-uri

                  :timestamp   event-timestamp
                  :content     {
                                :resource {:href "Run/45614147-aed1-4a24-889d-6365b0b1f2cd"}
                                :state    "Started"}
                  :type        "state"
                  :severity    "critical"
                  })

(defn valid? [event] (nil? (s/check Event event)))
(def invalid? (complement valid?))

(deftest check-event
  (is (= valid-event (crud/validate valid-event)))
  (is (valid? valid-event)))

(deftest check-severity
  (doseq [valid-severity ["critical" "high" "medium" "low"]]
    (is (valid? (assoc valid-event :severity valid-severity))))
  (is (invalid? (assoc valid-event :severity "unknown-severity"))))

(deftest check-type
  (doseq [valid-type ["state" "alarm"]]
    (is (valid? (assoc valid-event :type valid-type))))
  (is (invalid? (assoc valid-event :type "unknown-type"))))
