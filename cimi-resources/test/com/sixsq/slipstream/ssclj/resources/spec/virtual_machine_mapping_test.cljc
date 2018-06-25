(ns com.sixsq.slipstream.ssclj.resources.spec.virtual-machine-mapping-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.spec.virtual-machine-mapping :as vmm]
    [com.sixsq.slipstream.ssclj.resources.virtual-machine-mapping :as vmm-resource]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "test"
                         :type      "USER"
                         :right     "VIEW"}]})

(def timestamp "1964-08-25T10:00:00.0Z")
(def vm-sample {:id           "virtual-machine-mapping/mycloud-aaa-bbb-111"
                :resourceURI  vmm-resource/resource-uri
                :created      timestamp
                :updated      timestamp
                :acl          valid-acl
                :name         "short name"
                :description  "short description",
                :properties   {:a "one", :b "two"}

                :cloud        "mycloud"
                :instanceID   "aaa-bbb-111"

                :runUUID      "run/b836e665-74df-4800-89dc-c746c335a6a9"
                :owner        "user/jane"
                :serviceOffer {:href "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3"}})

(deftest test-schema-check
  (are [expect-fn arg] (expect-fn (s/valid? ::vmm/virtual-machine-mapping arg))
                       true? vm-sample
                       false? (assoc vm-sample :bad-attr {})
                       false? (assoc vm-sample :bad-attr "test"))

  ;; mandatory keywords
  (doseq [k #{:cloud :instanceID}]
    (is (not (s/valid? ::vmm/virtual-machine-mapping (dissoc vm-sample k)))))

  ;; optional keywords
  (doseq [k #{:runUUID :owner :serviceOffer}]
    (is (s/valid? ::vmm/virtual-machine-mapping (dissoc vm-sample k)))))
