(ns com.sixsq.slipstream.ssclj.resources.spec.storage-bucket-test
  (:require
    [clojure.test :refer [deftest]]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]
    [com.sixsq.slipstream.ssclj.resources.spec.storage-bucket :as bucky]
    [com.sixsq.slipstream.ssclj.resources.storage-bucket :as bucky-resource]))


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


(def bucky-sample {:id           (str bucky-resource/resource-url "/uuid")
                :resourceURI  bucky-resource/resource-uri
                :created      timestamp
                :updated      timestamp
                :acl          valid-acl
                :name         "short name"
                :description  "short description",
                :properties   {:a "one", :b "two"}
                :bucketName   "aaa-bbb-111"
                :usage        123.456
                :connector    {:href "connector/0123-4567-8912"}
                :credentials  [{:href  "connector/0123-4567-8912",
                                :roles ["realm:cern" "realm:my-accounting-group"]
                                :users ["long-user-id-1" "long-user-id-2"]}]
                :externalObject   {:href "external-object/aaa-bbb-ccc", :user {:href "user/test"}}
                :serviceOffer {:href                  "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3"
                               :resource:storage         1
                               :resource:host         "s3-eu-west-1.amazonaws.com"
                               :price:currency        "EUR"
                               :price:unitCode        "HUR"
                               :price:unitCost        "0.018"
                               :resource:platform "S3"}})


(deftest test-schema-check

  (stu/is-valid ::bucky/storage-bucket bucky-sample)
  (stu/is-valid ::bucky/storage-bucket (assoc bucky-sample :externalObject {:href "external-object/fff-42"}))
  (stu/is-valid ::bucky/storage-bucket (assoc bucky-sample :name "name"))

  ;; TODO: Remove when credential resource is being used
  (stu/is-valid ::bucky/storage-bucket (assoc bucky-sample :credentials [{:href "user/123456789@toto-aai.chhttps://aai-login.toto-aai.com/idp/shibboleth!https://fed-id.nuv.la/samlbridge/module.php/saml/sp/metadata.php/sixsq-saml-bridge!qwertyuiopasdfghjklzxcvbnm1234567890="}]))

  (stu/is-invalid ::bucky/storage-bucket (assoc bucky-sample :bad-attr {}))
  (stu/is-invalid ::bucky/storage-bucket (assoc bucky-sample :bad-attr "test"))

  ;; mandatory keywords
  (doseq [k #{:credentials :bucketName :connector :usage}]
    (stu/is-invalid ::bucky/storage-bucket (dissoc bucky-sample k)))

  ;; optional keywords
  (doseq [k #{:externalObject :serviceOffer}]
    (stu/is-valid ::bucky/storage-bucket (dissoc bucky-sample k))))
