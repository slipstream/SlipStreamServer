(ns com.sixsq.slipstream.ssclj.resources.spec.notification-test
  (:require
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.notification :refer :all]
    [com.sixsq.slipstream.ssclj.resources.spec.notification :as notification]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def notification-timestamp "2015-01-16T08:05:00.0Z")


(def valid-notification
  {:id          "notification/262626262626262"
   :resourceURI resource-uri
   :acl         {:owner {:type "USER" :principal "joe"}
                 :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}

   :timestamp   notification-timestamp
   :content     {:resource {:href "module/HNSciCloud-RHEA/S3"}
                 :state    "Started"}
   :type        "state"
   :severity    "critical"})


(deftest check-reference
  (let [updated-notification (assoc-in valid-notification [:content :resource :href] "another/valid-identifier")]
    (stu/is-valid ::notification/notification updated-notification))
  (let [updated-notification (assoc-in valid-notification [:content :resource :href] "/not a valid reference/")]
    (stu/is-invalid ::notification/notification updated-notification)))


(deftest check-severity
  (doseq [valid-severity ["critical" "high" "medium" "low"]]
    (stu/is-valid ::notification/notification (assoc valid-notification :severity valid-severity)))
  (stu/is-invalid ::notification/notification (assoc valid-notification :severity "unknown-severity")))


(deftest check-type
  (doseq [valid-type ["state" "alarm"]]
    (stu/is-valid ::notification/notification (assoc valid-notification :type valid-type)))
  (stu/is-invalid ::notification/notification (assoc valid-notification :type "unknown-type")))


(deftest check-notification-schema

  (stu/is-valid ::notification/notification valid-notification)

  ;; mandatory keywords
  (doseq [k #{:id :resourceURI :acl :timestamp :content :type :severity}]
    (stu/is-invalid ::notification/notification (dissoc valid-notification k)))

  ;; optional keywords
  (doseq [k #{}]
    (stu/is-valid ::notification/notification (dissoc valid-notification k))))
