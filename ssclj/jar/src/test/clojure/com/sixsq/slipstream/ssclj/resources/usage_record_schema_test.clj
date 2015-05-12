(ns com.sixsq.slipstream.ssclj.resources.usage-record-schema-test
  (require
    [com.sixsq.slipstream.ssclj.resources.usage-record  :refer :all]
    [schema.core                                        :as s]
    [clojure.test                                       :refer :all]))

(defn valid?
  [ur]
  (nil? (s/check UsageRecord ur)))

(def invalid? (complement valid?))

(def valid-usage-record
  { :acl {
          :owner {
                  :type "USER" :principal "joe"}
          :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}

    :id                     "Event/262626262626262"
    :resourceURI            resource-uri
    :cloud_vm_instanceid    "exoscale-ch-gva:9010d739-6933-4652-9db1-7bdafcac01cb"
    :user                   "joe"
    :cloud                  "aws"
    :start_timestamp        "2015-05-04T15:32:22.853Z"
    :metric_name            "vm"
    :metric_value           "1.0"})

(def valid-usage-records
  [valid-usage-record
   (assoc valid-usage-record :end_timestamp "2015-05-04T16:32:22.853Z")
   (assoc valid-usage-record :end_timestamp "")])

(deftest test-schema
  (is (true? (every? valid? valid-usage-records))))