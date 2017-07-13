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
               root {:id           (str acc/resource-url "/uuid")
                     :resourceURI  acc/resource-uri
                     :created      timestamp
                     :updated      timestamp
                     :acl          valid-acl

                     ;; common accounting record attributes
                     :type         "vm"
                     :identifier   "my-cloud-vm-47"
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
              (is (s/valid? :cimi/accounting-record.vm root))
              (doseq [k #{:cpu :ram :instanceType}]
                     (is (not (s/valid? :cimi/accounting-record.vm (dissoc root k)))))
              (doseq [k #{:disk}]
                     (is (s/valid? :cimi/accounting-record.vm (dissoc root k))))
              ;;parent mandatory keywords
              (doseq [k #{:identifier :start :user :type :cloud}]
                     (is (not (s/valid? :cimi/accounting-record.vm (dissoc root k)))))
              ;;parent optional keywords
              (doseq [k #{:stop :roles :groups :realm :module :serviceOffer}]
                     (is (s/valid? :cimi/accounting-record.vm (dissoc root k)))
                     ))
         )



