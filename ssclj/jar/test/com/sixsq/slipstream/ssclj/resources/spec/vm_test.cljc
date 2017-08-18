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

(def timestamp "1964-08-25T10:00:00.0Z")
(def vm-sample {:id           (str vm/resource-url "/uuid")
                :resourceURI  vm/resource-uri
                :created      timestamp
                :updated      timestamp
                :acl          valid-acl


                :user         {:href "user/test"}
                :connector    {:href "connector/scissor-fr1"}
                :serviceOffer {:href "service-offer/my-uuid"
                               :resource:vcpu 1
                               :resource:ram 4096
                               :resource:disk 10
                               :resource:instanceType "Large"
                               }
                :instanceId "aaa-ddd-bbb-42"
                :state "Running"
                })

(deftest test-schema-check


    (s/explain-data :cimi/vm vm-sample)

    (are [expect-fn arg] (expect-fn (s/valid? :cimi/vm arg))
                         true? vm-sample
                         true? (assoc vm-sample :ip "126.98.56.5")
                         false? (assoc vm-sample :user {})
                         false? (assoc vm-sample :user "test")
                         false? (assoc vm-sample :connector {})
                         false? (assoc vm-sample :connector "scissor-fr1")
                         true? (assoc vm-sample :run {:href "run/fff-42"})
                         true? (assoc vm-sample :nodeName "node name")
                         true? (assoc vm-sample :name "name")
                         true? (assoc vm-sample :nodeInstanceId "aaa-bbb-111")
                         true? (assoc vm-sample :usable true)
                         )


    ;;mandatory keywords
    (doseq [k #{ :connector :user :state :instanceId }]
      (is (not (s/valid? :cimi/vm (dissoc vm-sample k)))))

    ;; optional keywords
    (doseq [k #{:run  :serviceOffer :ip :nodeName :name :nodeInstanceId :usable}]
      (is (s/valid? :cimi/vm (dissoc vm-sample k)))
      )

  )



