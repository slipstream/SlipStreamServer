(ns com.sixsq.slipstream.ssclj.resources.spec.vm-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]

    [com.sixsq.slipstream.ssclj.resources.spec.vm :as t]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.vm :as vm]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        vm-sample {:id           (str vm/resource-url "/uuid")
                           :resourceURI  vm/resource-uri
                           :created      timestamp
                           :updated      timestamp
                           :acl          valid-acl


                           :user         "some-complicated-user-id"
                           :cloud        "my-cloud"

                           :cpu          1
                           :ram          1024
                           :disk         10
                           :instanceId   "aaa-bbb-111"
                           :instanceType "Large"
                           :state "Running"
                           }]

    (s/explain-data :cimi/vm vm-sample)

    (are [expect-fn arg] (expect-fn (s/valid? :cimi/vm arg))
                         true? vm-sample
                         true? (assoc vm-sample :ip "126.98.56.5")
                         ;TODO false? (assoc vm-sample :run {})
                         true? (assoc vm-sample :run {:href "run/fff-42"})
                         true? (assoc vm-sample :nodeName "node name")
                         true? (assoc vm-sample :name "name")
                         true? (assoc vm-sample :nodeInstanceId "aaa-bbb-111")
                         true? (assoc vm-sample :usable true)
                         )


    ;;mandatory keywords
    (doseq [k #{ :cloud :user :state :instanceId :instanceType  :cpu :ram :disk}]
      (is (not (s/valid? :cimi/vm (dissoc vm-sample k)))))

    ;; optional keywords
    (doseq [k #{:run  :ip :nodeName :name :nodeInstanceId :usable}]
      (is (s/valid? :cimi/vm (dissoc vm-sample k)))
      )
    )
  )



