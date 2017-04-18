(ns com.sixsq.slipstream.ssclj.resources.spec.usage-record-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.usage-record :refer :all]))

(defn valid? [resource] (s/valid? :cimi/usage-record resource))
(def invalid? (complement valid?))

(def valid-usage-record
  {:acl                 {:owner {:type "USER", :principal "joe"}
                         :rules [{:type "ROLE", :principal "ANON", :right "ALL"}]}

   :id                  "usage-record/be23a1ba-0161-4a9a-b1e1-b2f4164e9a02"
   :resourceURI         resource-uri
   :cloud-vm-instanceid "exoscale-ch-gva:9010d739-6933-4652-9db1-7bdafcac01cb"
   :user                "joe"
   :cloud               "aws"
   :start-timestamp     "2015-05-04T15:32:22.853Z"
   :metric-name         "vm"
   :metric-value        "1.0"})

(def check-schema
  (are [expect-fn arg] (expect-fn arg)
                       valid? valid-usage-record
                       valid? (assoc valid-usage-record :end-timestamp "2015-05-04T16:32:22.853Z")
                       valid? (assoc valid-usage-record :end-timestamp "")
                       invalid? (-> valid-usage-record
                                    (dissoc :cloud-vm-instanceid)
                                    (assoc :cloud_vm_instanceid "123"))))

