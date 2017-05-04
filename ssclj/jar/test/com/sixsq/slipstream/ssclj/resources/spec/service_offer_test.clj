(ns com.sixsq.slipstream.ssclj.resources.spec.service-offer-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.service-offer :as so]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest check-ServiceInfo
  (let [timestamp "1964-08-25T10:00:00.0Z"
        service-offer {:id          (str so/resource-url "/offer-uuid")
                       :resourceURI so/resource-uri
                       :created     timestamp
                       :updated     timestamp
                       :acl         valid-acl
                       :connector   {:href "myconnector"}
                       :other       "value"}]

    (are [expect-fn arg] (expect-fn (s/valid? :cimi/service-offer arg))
                         true? service-offer
                         false? (dissoc service-offer :created)
                         false? (dissoc service-offer :updated)
                         false? (dissoc service-offer :acl)
                         false? (dissoc service-offer :connector)
                         true? (dissoc service-offer :other))))
