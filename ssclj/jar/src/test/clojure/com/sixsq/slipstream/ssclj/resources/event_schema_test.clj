(ns com.sixsq.slipstream.ssclj.resources.event-schema-test
  (:require
    [schema.core                                          :as s]
    [expectations                                         :refer :all]
    [com.sixsq.slipstream.ssclj.resources.event           :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud     :as crud]))

(def event-timestamp "2015-01-16T08:05:00.0Z")

(def valid-event {
  :acl {
    :owner {
      :type "USER" :principal "joe"}
    :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}

  :id "Event/262626262626262"

  :resourceURI resource-uri

  :timestamp event-timestamp
  :content  {
    :resource {:href "Run/45614147-aed1-4a24-889d-6365b0b1f2cd"}
    :state "Started"}
  :type "state"
  :severity "critical"
})

(expect valid-event (crud/validate valid-event))

(defn valid? [event] (nil? (s/check Event event)))
(defn invalid? [event] (complement valid?))

(expect (valid? valid-event))

(doseq [valid-severity ["critical" "high" "medium" "low"]]
  (expect (valid? (assoc valid-event :severity valid-severity))))
(expect (invalid? (assoc valid-event :severity "unknown-severity")))

(doseq [valid-type ["state" "alarm"]]
  (expect (valid? (assoc valid-event :type valid-type))))
(expect (invalid? (assoc valid-event :type "unknown")))
