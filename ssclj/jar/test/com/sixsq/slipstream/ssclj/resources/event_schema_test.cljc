(ns com.sixsq.slipstream.ssclj.resources.event-schema-test
  (:require
    [clojure.test :refer [deftest is]]
    [clojure.spec :as spec]
    [com.sixsq.slipstream.ssclj.resources.event :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.event.spec :as schema]))

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

(defn valid? [event] (spec/valid? ::schema/event event))
(def invalid? (complement valid?))

(deftest valid-event-returned
         (is (= valid-event (crud/validate valid-event))))

(deftest valid-works
  (is (valid? valid-event)))

(deftest check-severity
  (doseq [valid-severity ["critical" "high" "medium" "low"]]
    (is (valid? (assoc valid-event :severity valid-severity))))
  (is (invalid? (assoc valid-event :severity "unknown-severity"))))

(deftest check-type
  (doseq [valid-type ["state" "alarm" "action" "system"]]
    (is (valid? (assoc valid-event :type valid-type))))
  (is (invalid? (assoc valid-event :type "unknown-type"))))
