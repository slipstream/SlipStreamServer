(ns com.sixsq.slipstream.ssclj.resources.spec.virtual-machine-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]

    [com.sixsq.slipstream.ssclj.resources.spec.virtual-machine :as t]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.virtual-machine :as vm]))

(def valid-acl {:owner {:principal "ADMIN",
                        :type      "ROLE"},
                :rules [{:principal "realm:accounting_manager",
                         :type      "ROLE",
                         :right     "VIEW"},
                        {:principal "test",
                         :type      "USER",
                         :right     "VIEW"},
                        {:principal "cern:cern",
                         :type      "ROLE",
                         :right     "VIEW"},
                        {:principal "cern:my-accounting-group",
                         :type      "ROLE",
                         :right     "VIEW"}]})

(def timestamp "1964-08-25T10:00:00.0Z")
(def vm-sample {:id           (str vm/resource-url "/uuid")
                :resourceURI  vm/resource-uri
                :created      timestamp
                :updated      timestamp
                :acl          valid-acl

                :name         "short name"
                :description  "short description",
                :properties   {:a "one",
                               :b "two"}

                :instanceID   "aaa-bbb-111"
                :state        "Running"
                :ip           "127.0.0.1"

                :credentials  [{:href  "connector/0123-4567-8912",
                                :roles ["realm:cern", "realm:my-accounting-group"]
                                :users ["long-user-id-1", "long-user-id-2"]}
                               ]
                :run          {:href "run/aaa-bbb-ccc",
                               :user {:href "user/test"}}

                :serviceOffer {:href                  "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3"
                               :resource:vcpu         1
                               :resource:ram          4096
                               :resource:disk         10
                               :resource:instanceType "Large"}})

(deftest test-schema-check

  #_(s/explain-data :cimi/virtual-machine vm-sample)

  (are [expect-fn arg] (expect-fn (s/valid? :cimi/virtual-machine arg))
                       true? vm-sample

                       false? (assoc vm-sample :bad-attr {})
                       false? (assoc vm-sample :bad-attr "test")

                       true? (assoc vm-sample :run {:href "run/fff-42"})
                       true? (assoc vm-sample :name "name"))

  ;; mandatory keywords
  (doseq [k #{:credentials :state :instanceID}]
    (is (not (s/valid? :cimi/virtual-machine (dissoc vm-sample k)))))

  ;; optional keywords
  (doseq [k #{:run :serviceOffer :ip}]
    (is (s/valid? :cimi/virtual-machine (dissoc vm-sample k)))))
