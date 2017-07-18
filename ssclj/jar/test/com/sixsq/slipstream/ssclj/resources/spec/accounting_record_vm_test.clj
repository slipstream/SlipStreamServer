(ns com.sixsq.slipstream.ssclj.resources.spec.accounting-record-vm-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]

    [com.sixsq.slipstream.ssclj.resources.spec.accounting-record-vm :as t]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.accounting-record :as acc]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        accounting-record {:id           (str acc/resource-url "/uuid")
                           :resourceURI  acc/resource-uri
                           :created      timestamp
                           :updated      timestamp
                           :acl          valid-acl

                           ;; common accounting record attributes
                           :type         "vm"
                           :start        timestamp
                           :stop         timestamp
                           :user         "some-complicated-user-id"
                           :cloud        "my-cloud"
                           :roles        ["a" "b" "c"]
                           :groups       ["g1" "g2" "g3"]
                           :realm        "my-organization"
                           :module       "module/example/images/centos-7"
                           :serviceOffer {:href "service-offer/my-uuid"}

                           ;; vm subtype
                           :cpu          1
                           :ram          1024
                           :disk         10
                           :instanceType "myInstanceType"
                           }]
    (are [expect-fn arg] (expect-fn (s/valid? :cimi/accounting-record.vm arg))
                         true? accounting-record
                         true? (assoc accounting-record :context {})
                         true? (assoc accounting-record :context {:abc "abc"})
                         false? (assoc accounting-record :context {:abc 2})
                         false? (assoc accounting-record :context {:def {}})
                         )
    #_(doseq [k #{}]
      (is (not (s/valid? :cimi/accounting-record.vm (dissoc accounting-record k)))))
    (doseq [k #{:disk}]
      (is (s/valid? :cimi/accounting-record.vm (dissoc accounting-record k))))
    ;;parent mandatory keywords
    (doseq [k #{ :start :user :type :cloud}]
      (is (not (s/valid? :cimi/accounting-record.vm (dissoc accounting-record k)))))
    ;;parent optional keywords
    (doseq [k #{:stop :roles :groups :realm :module :serviceOffer}]
      (is (s/valid? :cimi/accounting-record.vm (dissoc accounting-record k)))
      )
    )
  )



