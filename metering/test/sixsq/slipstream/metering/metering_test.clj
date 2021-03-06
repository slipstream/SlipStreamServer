(ns sixsq.slipstream.metering.metering-test
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer [are deftest is]]
    [sixsq.slipstream.metering.metering :as t]
    [sixsq.slipstream.metering.utils :as utils]))


(deftest check-es-hosts
  (is (= ["http://a:1234"] (t/es-hosts "a" 1234)))
  (is (= ["http://a:1234"] (t/es-hosts "a" "1234"))))


(deftest check-index-action
  (is (= {:index {:_index "index", :_type "type"}}
         (t/index-action "index" "type"))))


(deftest check-search-url
  (is (= "index/type/_search" (t/search-url "index" "type"))))

(deftest check-search-urls
  (is (= #{"index1/type1/_search" "index2/type2/_search"} (set (t/search-urls ["index1" "index2"] ["type1" "type2"])))))


(deftest check-process-options
  (let [{:keys [hosts
                resource-search-urls
                metering-action
                metering-period-minutes]
         :as   options}
        (t/process-options {})]

    (is (= 4 (count options)))
    (is (= ["http://127.0.0.1:9200"] hosts))
    (is (= #{"slipstream-virtual-machine/_doc/_search" "slipstream-storage-bucket/_doc/_search"} (set resource-search-urls)))
    (is (= "slipstream-metering" (-> metering-action :index :_index)))
    (is (= t/doc-type (-> metering-action :index :_type)))
    (is (= 1 metering-period-minutes)))

  (is (= ["http://elasticsearch:1234"] (:hosts (t/process-options {:es-host "elasticsearch"
                                                                   :es-port 1234}))))
  (is (= 2 (:metering-period-minutes (t/process-options {:metering-period-minutes 2}))))
  (is (= #{"alpha/_doc/_search" "beta/_doc/_search"} (set (:resource-search-urls (t/process-options {:vm-index "alpha" :bucky-index "beta"})))))

  (let [{:keys [metering-action]}
        (t/process-options {:metering-index "gamma"})]
    (is (= "gamma" (-> metering-action :index :_index)))))


(deftest check-assoc-snapshot-time
  (let [ts "2017-09-10T16:50:00.360Z"]
    (is (= {:snapshot-time ts} (t/assoc-snapshot-time ts {})))
    (is (= {:snapshot-time ts} (t/assoc-snapshot-time ts {:snapshot-time "BAD!"})))))



(deftest check-assoc-price
  (let [timestamp "1964-08-25T10:00:00.0Z"
        base-vm {:id          (str "metering/" (utils/random-uuid))
                 :resourceURI "http://sixsq.com/slipstream/1/VirtualMachine"
                 :created     timestamp
                 :updated     timestamp
                 :name        "short name"
                 :description "short description",
                 :properties  {:a "one",
                               :b "two"}
                 :instanceID  "aaa-bbb-111"
                 :connector   {:href "connector/0123-4567-8912"}
                 :state       "Running"
                 :ip          "127.0.0.1"
                 :credentials [{:href  "credential/0123-4567-8912",
                                :roles ["realm:cern", "realm:my-accounting-group"]
                                :users ["long-user-id-1", "long-user-id-2"]}]
                 :deployment  {:href "run/aaa-bbb-ccc",
                               :user {:href "user/test"}}}
        base-bucky {:id             (str "metering/" (utils/random-uuid))
                    :resourceURI    "http://sixsq.com/slipstream/1/StorageBucket"
                    :created        timestamp
                    :updated        timestamp

                    :name           "short name"
                    :description    "short description",
                    :properties     {:a "one",
                                     :b "two"}

                    :bucketName     "aaa-bbb-111"
                    :usageInKiB     12
                    :connector      {:href "connector/0123-4567-8912"}


                    :credentials    [{:href "credential/0123-4567-8912"}]


                    :externalObject {:href "external-object/aaa-bbb-ccc",
                                     :user {:href "user/test"}}}
        so-no-price {:href                  "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3"
                     :resource:vcpu         1
                     :resource:ram          4096
                     :resource:disk         10
                     :resource:type         "VM"
                     :resource:instanceType "Large"}
        so-minute {:price:currency          "EUR"
                   :resource:vcpu           8
                   :price:unitCost          0.4686389
                   :price:unitCode          "MIN"
                   :price:billingPeriodCode "MIN"
                   :price:freeUnits         0.0
                   :resource:instanceType   "s1.2xlarge"
                   :resource:ram            32768.0
                   :resource:disk           0.0
                   :resource:type           "VM"
                   :href                    " service-offer/a4953f05-affe-46d0-a5ac-a4c8f1af810b "}
        so-hour {:price:currency          "EUR"
                 :resource:vcpu           2
                 :price:unitCost          0.09122021
                 :price:unitCode          "HUR"
                 :price:billingPeriodCode "HUR"
                 :price:freeUnits         0.0
                 :resource:type           "VM"
                 :resource:instanceType   "t2.large"
                 :resource:ram            8192.0
                 :resource:disk           10.0
                 :href                    "service-offer/cc87133e-343b-40f1-8094-46f80a1b3042"}
        so-unknown-period {:price:currency          "EUR"
                           :resource:vcpu           2
                           :price:unitCost          0.09122021
                           :price:unitCode          "XXX"
                           :price:billingPeriodCode "XXX"
                           :price:freeUnits         0.0
                           :resource:instanceType   "t2.large"
                           :resource:ram            8192.0
                           :resource:disk           10.0
                           :resource:type           "VM"
                           :href                    "service-offer/cc87133e-343b-40f1-8094-46f80a1b3042"}
        so-unitCode-but-no-cost {:price:currency          "EUR"
                                 :resource:vcpu           8
                                 :price:unitCode          "MIN"
                                 :price:billingPeriodCode "MIN"
                                 :price:freeUnits         0.0
                                 :resource:instanceType   "s1.2xlarge"
                                 :resource:ram            32768.0
                                 :resource:disk           0.0
                                 :resource:type           "VM"
                                 :href                    " service-offer/a4953f05-affe-46d0-a5ac-a4c8f1af810b "}
        so-unknown {:href          "service-offer/unknown"
                    :resource:vcpu 1
                    :resource:ram  512.0
                    :resource:disk 10.0}
        s3cost 0.018
        sample-usage-KiB (* 42 1024 1024)                   ;; i.e 42 Gb

        so-sb-gibh {:connector               {
                                              :href "exoscale-ch-dk"
                                              },
                    :description             "Data volume (objects) stored in S3 in all Exoscale zones",
                    :price:currency          "EUR",
                    :resource:storage        1,
                    :price:unitCost          s3cost,

                    :price:billingPeriodCode "MON",
                    :name                    "Object Storage in Exoscale",

                    :price:freeUnits         0,
                    :resource:host           "sos.exo.io",

                    :resource:country        "CH",
                    :resource:platform       "S3",
                    :resourceURI             "http://sixsq.com/slipstream/1/ServiceOffer",
                    :resource:type           "DATA",
                    :price:billingUnit       "GiBh"}
        so-sb-mibh (assoc so-sb-gibh :price:billingUnit "MiBh")]
    (is (= {} (t/assoc-price {})))
    (is (nil? (:price (t/assoc-price {::bad " BAD! "}))))
    (is (nil? (:price (t/assoc-price {:serviceOffer " BAD! "}))))
    (is (nil? (:price (t/assoc-price base-vm))))
    (is (nil? (:price (t/assoc-price (assoc base-vm :serviceOffer so-no-price)))))
    (is (= (:price:unitCost so-minute) (:price (t/assoc-price (assoc base-vm :serviceOffer so-minute)))))
    (is (= (/ (:price:unitCost so-hour) 60) (:price (t/assoc-price (assoc base-vm :serviceOffer so-hour)))))
    (is (nil? (:price (t/assoc-price (assoc base-vm :serviceOffer so-unknown-period)))))
    (is (nil? (:price (t/assoc-price (assoc base-vm :serviceOffer so-unitCode-but-no-cost)))))
    (is (nil? (:price (t/assoc-price (assoc base-vm :serviceOffer so-unknown)))))
    (is (nil? (:price (t/assoc-price base-bucky))))
    (is (nil? (:price (t/assoc-price (assoc base-bucky :serviceOffer so-no-price)))))
    (is (nil? (:price (t/assoc-price (assoc base-bucky :serviceOffer so-unknown-period)))))
    (is (nil? (:price (t/assoc-price (assoc base-bucky :serviceOffer so-unitCode-but-no-cost)))))
    (is (nil? (:price (t/assoc-price (assoc base-bucky :serviceOffer so-unknown)))))

    ;;storage bucket with billing in GiBH
    (is (= (-> sample-usage-KiB
               (/ 60)
               (* s3cost)
               (/ 1024)
               (/ 1024))
           (:price (t/assoc-price (assoc base-bucky :serviceOffer so-sb-gibh :usageInKiB sample-usage-KiB)))))

    ;;storage bucket with billing in MiBH
    (is (= (-> sample-usage-KiB
               (/ 60)
               (* s3cost)
               (/ 1024))
           (:price (t/assoc-price (assoc base-bucky :serviceOffer so-sb-mibh :usageInKiB sample-usage-KiB)))))))


(deftest check-assoc-type
  (let [timestamp "1964-08-25T10:00:00.0Z"
        base-vm {:id          (str "metering/" (utils/random-uuid))
                 :resourceURI "http://sixsq.com/slipstream/1/VirtualMachine"
                 :created     timestamp
                 :updated     timestamp
                 :name        "short name"
                 :description "short description",
                 :properties  {:a "one",
                               :b "two"}
                 :instanceID  "aaa-bbb-111"
                 :connector   {:href "connector/0123-4567-8912"}
                 :state       "Running"
                 :ip          "127.0.0.1"
                 :credentials [{:href  "credential/0123-4567-8912",
                                :roles ["realm:cern", "realm:my-accounting-group"]
                                :users ["long-user-id-1", "long-user-id-2"]}]
                 :deployment  {:href "run/aaa-bbb-ccc",
                               :user {:href "user/test"}}}
        base-bucky {:id             (str "metering/" (utils/random-uuid))
                    :resourceURI    "http://sixsq.com/slipstream/1/StorageBucket"
                    :created        timestamp
                    :updated        timestamp

                    :name           "short name"
                    :description    "short description",
                    :properties     {:a "one",
                                     :b "two"}

                    :bucketName     "aaa-bbb-111"
                    :usageInKiB     12
                    :connector      {:href "connector/0123-4567-8912"}


                    :credentials    [{:href "credential/0123-4567-8912"}]


                    :externalObject {:href "external-object/aaa-bbb-ccc",
                                     :user {:href "user/test"}}}

        so-bucky-with-type {:price:currency          "EUR",
                            :resource:storage        1,
                            :price:unitCost          0.0018,
                            :price:billingPeriodCode "MON",
                            :name                    "Object Storage in Exoscale",
                            :price:freeUnits         0,
                            :resource:host           "sos.exo.io",
                            :resource:country        "CH",
                            :resource:platform       "S3",
                            :resourceURI             "http://sixsq.com/slipstream/1/ServiceOffer",
                            :resource:type           "DATA",
                            :price:billingUnit       "GiBh"}

        so-hour {:price:currency          "EUR"
                 :resource:vcpu           2
                 :price:unitCost          0.09122021
                 :price:unitCode          "HUR"
                 :price:billingPeriodCode "HUR"
                 :price:freeUnits         0.0
                 :resource:type           "VM"
                 :resource:instanceType   "t2.large"
                 :resource:ram            8192.0
                 :resource:disk           10.0
                 :href                    "service-offer/cc87133e-343b-40f1-8094-46f80a1b3042"}
        so-unknown-period {:price:currency          "EUR"
                           :resource:vcpu           2
                           :price:unitCost          0.09122021
                           :price:unitCode          "XXX"
                           :price:billingPeriodCode "XXX"
                           :price:freeUnits         0.0
                           :resource:instanceType   "t2.large"
                           :resource:ram            8192.0
                           :resource:disk           10.0
                           :resource:type           "VM"
                           :href                    "service-offer/cc87133e-343b-40f1-8094-46f80a1b3042"}
        so-unitCode-but-no-cost {:price:currency          "EUR"
                                 :resource:vcpu           8
                                 :price:unitCode          "MIN"
                                 :price:billingPeriodCode "MIN"
                                 :price:freeUnits         0.0
                                 :resource:instanceType   "s1.2xlarge"
                                 :resource:ram            32768.0
                                 :resource:disk           0.0
                                 :resource:type           "VM"
                                 :href                    " service-offer/a4953f05-affe-46d0-a5ac-a4c8f1af810b "}
        so-no-type {:href          "service-offer/unknown"
                    :resource:vcpu 1
                    :resource:ram  512.0
                    :resource:disk 10.0}]

    (is (= {} (t/assoc-type {})))
    (is (nil? (:resource:type (t/assoc-type {::bad " BAD! "}))))
    (is (nil? (:resource:type (t/assoc-type {:serviceOffer " BAD! "}))))
    (is (nil? (:resource:type (t/assoc-type base-vm))))
    (is (= "VM" (:resource:type (t/assoc-type (assoc base-vm :serviceOffer so-hour)))))
    (is (= "VM" (:resource:type (t/assoc-type (assoc base-vm :serviceOffer so-unknown-period)))))
    (is (= "VM" (:resource:type (t/assoc-type (assoc base-vm :serviceOffer so-unitCode-but-no-cost)))))
    (is (nil? (:resource:type (t/assoc-type (assoc base-vm :serviceOffer so-no-type)))))
    (is (nil? (:resource:type (t/assoc-type base-bucky))))
    (is (= "DATA" (:resource:type (t/assoc-type (assoc base-bucky :serviceOffer so-bucky-with-type)))))
    (is (nil? (:resource:type (t/assoc-type (assoc base-bucky :serviceOffer so-no-type)))))))


(deftest check-update-id
  (let [uuid "5b24caac-e87c-4446-96bc-a20b21450a1"
        vm-id (str "virtual-machine/" uuid)
        ts "2017-09-10T16:50:00.360Z"
        metering-id (str "metering/" uuid "-" (str/replace ts #"[:\.]" "-"))]
    (is (not (nil? (:id (t/update-id ts {})))))
    (is (= {:id metering-id} (t/update-id ts {:id vm-id})))))


(deftest check-replace-resource-uri
  (is (= {:resourceURI t/metering-resource-uri} (t/replace-resource-uri {})))
  (is (= {:resourceURI t/metering-resource-uri} (t/replace-resource-uri {:resourceURI "BAD!"}))))


(deftest check-create-actions
  (doseq [search-result-doc [{:body {:hits {:hits [{:_source {:a 1}}
                                                   {:_source {:b 2}}]}}}
                             (-> (io/resource "virtual-machines.json")
                                 slurp
                                 (json/read-str :key-fn keyword))]]
    (let [
          action (t/index-action "index" "type")
          actions (t/create-actions "timestamp" action search-result-doc)]
      (is (pos? (count actions)))
      (is (->> actions
               (map second)
               (map :snapshot-time)
               (every? #(= "timestamp" %)))))))


(deftest check-response-stats
  (is (= [0 {}] (t/response-stats (ex-info "some error" {}))))
  (let [n 100
        job (vec (repeat (* 2 n) :job))
        resp-maps-200 (vec (repeat n {:index {:status 200}}))
        resp-maps-400 (vec (repeat n {:index {:status 400}}))
        responses {:body {:items (vec (concat resp-maps-200 resp-maps-400))}}]
    (is (= (* 2 n) (first (t/response-stats [job responses]))))
    (is (= {200 n, 400 n} (second (t/response-stats [job responses]))))))


(deftest check-merge-stats
  (is (= [0 {}] (t/merge-stats)))
  (is (= [13 {200 10, 400 3}] (t/merge-stats [13 {200 10, 400 3}])))
  (is (= [15 {200 7, 300 5, 400 3}] (t/merge-stats [5 {200 2, 400 3}]
                                                   [10 {200 5, 300 5}]))))
