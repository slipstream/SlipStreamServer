(ns com.sixsq.slipstream.ssclj.resources.usage-record-schema-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.usage-record :refer :all]
    [com.sixsq.slipstream.ssclj.resources.test-utils :as tu]))

(def valid-usage-record
  {:acl                 {
                         :owner {
                                 :type "USER" :principal "joe"}
                         :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}

   :id                  "UsageRecord/be23a1ba-0161-4a9a-b1e1-b2f4164e9a02"
   :resourceURI         resource-uri
   :cloud-vm-instanceid "exoscale-ch-gva:9010d739-6933-4652-9db1-7bdafcac01cb"
   :user                "joe"
   :cloud               "aws"
   :start-timestamp     "2015-05-04T15:32:22.853Z"
   :metric-name         "vm"
   :metric-value        "1.0"})

(def valid-usage-records
  [valid-usage-record
   (assoc valid-usage-record :end-timestamp "2015-05-04T16:32:22.853Z")
   (assoc valid-usage-record :end-timestamp "")])

(deftest test-schema
  (doseq [record valid-usage-records]
    (is (= record (crud/validate record)))))

(deftest test-invalid-records
  (doseq [record (map (fn [r] (-> r
                                  (dissoc :cloud-vm-instanceid)
                                  (assoc :cloud_vm_instanceid "123")))
                      valid-usage-records)]
    (is (thrown? Exception (crud/validate record)))))
