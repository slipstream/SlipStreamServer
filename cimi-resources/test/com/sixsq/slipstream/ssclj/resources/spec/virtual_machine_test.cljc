(ns com.sixsq.slipstream.ssclj.resources.spec.virtual-machine-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.spec.virtual-machine :as vm]
    [com.sixsq.slipstream.ssclj.resources.virtual-machine :as vm-resource]))

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
(def vm-sample {:id           (str vm-resource/resource-url "/uuid")
                :resourceURI  vm-resource/resource-uri
                :created      timestamp
                :updated      timestamp
                :acl          valid-acl
                :name         "short name"
                :description  "short description",
                :properties   {:a "one", :b "two"}
                :instanceID   "aaa-bbb-111"
                :connector    {:href "connector/0123-4567-8912"}
                :state        "Running"
                :ip           "127.0.0.1"
                :credentials  [{:href  "connector/0123-4567-8912",
                                :roles ["realm:cern", "realm:my-accounting-group"]
                                :users ["long-user-id-1", "long-user-id-2"]}]
                :deployment   {:href "run/aaa-bbb-ccc", :user {:href "user/test"}}
                :serviceOffer {:href                  "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3"
                               :resource:vcpu         1
                               :resource:ram          4096
                               :resource:disk         10
                               :resource:instanceType "Large"}})

(deftest test-schema-check
  (are [expect-fn arg] (expect-fn (s/valid? ::vm/virtual-machine arg))
                       true? vm-sample
                       false? (assoc vm-sample :bad-attr {})
                       false? (assoc vm-sample :bad-attr "test")
                       true? (assoc vm-sample :deployment {:href "run/fff-42"})
                       true? (assoc vm-sample :name "name"))

                       ; TODO: Remove when credential resource is being used
                       true? (assoc vm-sample :credentials [{:href "user/123456789@toto-aai.chhttps://aai-login.toto-aai.com/idp/shibboleth!https://fed-id.nuv.la/samlbridge/module.php/saml/sp/metadata.php/sixsq-saml-bridge!qwertyuiopasdfghjklzxcvbnm1234567890="}])

  ;; mandatory keywords
  (doseq [k #{:credentials :state :instanceID :connector}]
    (is (not (s/valid? ::vm/virtual-machine (dissoc vm-sample k)))))

  ;; optional keywords
  (doseq [k #{:deployment :serviceOffer :ip}]
    (is (s/valid? ::vm/virtual-machine (dissoc vm-sample k)))))
