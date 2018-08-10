(ns sixsq.slipstream.metering.metering-live-test
  (:require
    [clojure.core.async :refer [<!!]]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.dbtest.es.spandex-utils :as spu]
    [qbits.spandex :as spandex]
    [sixsq.slipstream.metering.metering :as t]
    [sixsq.slipstream.metering.utils :as utils]))

(defn random-vm-doc
  [resource-type]
  (let [doc-id (str resource-type "/" (utils/random-uuid))
        instance-id (utils/random-uuid)
        cloud (rand-nth ["connector/cloud-1" "connector/cloud-2" "connector/cloud-3"])
        user (rand-nth ["user-1" "user-2" "user-3"])]
    {:id           doc-id
     :resourceURI  "http://sixsq.com/slipstream/1/VirtualMachine"
     :updated      "2017-09-04T09:39:35.679Z"
     :credential   {:href cloud}
     :created      "2017-09-04T09:39:35.651Z"
     :state        "Running"
     :instanceID   instance-id
     :serviceOffer {:href                  "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3"
                    :resource:vcpu         1
                    :resource:ram          4096
                    :resource:disk         10
                    :resource:instanceType "Large"}
     :deployment   {:href "run/4824efe2-59e9-4db6-be6b-fc1c8b3edf40"
                    :user {:href (str "user/" user)}}
     :acl          {:owner {:type      "USER"
                            :principal "ADMIN"}
                    :rules [{:principal "ADMIN"
                             :right     "ALL"
                             :type      "USER"}
                            {:principal user
                             :right     "VIEW"
                             :type      "USER"}]}}))

(defn random-bucky-doc
  [resource-type]
  (let [doc-id (str resource-type "/" (utils/random-uuid))
        cred (rand-nth ["credential/123" "credential/456" "credential/789"])
        user (rand-nth ["user-1" "user-2" "user-3"])]
    {:id           doc-id
     :resourceURI  "http://sixsq.com/slipstream/1/StorageBucket"
     :updated      "2017-09-04T09:39:35.679Z"
     :credentials   [{:href cred}]
     :created      "2017-09-04T09:39:35.651Z"
     :serviceOffer   {:href              "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3"
                      :resource:storage  1
                      :resource:host     "s3-eu-west-1.amazonaws.com"
                      :price:currency    "EUR"
                      :price:unitCode    "HUR"
                      :price:unitCost    0.018
                      :resource:platform "S3"}
     :bucketName    (utils/random-uuid)
     :usage          123456
     :connector      {:href "connector/0123-4567-8912"}
     :externalObject {:href "external-object/aaa-bbb-ccc",
                      :user {:href "user"}}
     :acl          {:owner {:type      "USER"
                            :principal "ADMIN"}
                    :rules [{:principal "ADMIN"
                             :right     "ALL"
                             :type      "USER"}
                            {:principal user
                             :right     "VIEW"
                             :type      "USER"}]}}))




(deftest check-empty-bulk-insert
  (let [hosts ["http://localhost:9200"]]
    (with-open [client (spandex/client {:hosts hosts})]
      (let [ch (t/bulk-insert client [])]
        (is (nil? (<!! ch)))))))

(deftest lifecycle
  (let [resource-index1 (utils/random-uuid)
        resource-index2 (utils/random-uuid)
        resource-type1 "virtual-machine"
        resource-type2 "bucky"

        ;;resource-search-url (t/search-url resource-index "virtual-machine")

        ;;resource-search-urls [resource-search-url]
        resource-search-urls (t/search-urls [resource-index1 resource-index2] [resource-type1 resource-type2])

        metering-index (utils/random-uuid)
        metering-type "metering"
        metering-action (t/index-action metering-index metering-type)
        metering-search-url (t/search-url metering-index metering-type)
        rest-map (spu/provide-mock-rest-client)]
    (with-open [client (:client rest-map)]
      (when (spu/cluster-ready? client)
        (spu/index-create client resource-index1)
        (try
          (let [n1 199
                n2 42
                docs1 (repeatedly n1 (partial random-vm-doc resource-type1))
                docs2 (repeatedly n2 (partial random-bucky-doc resource-type2))
                ids1 (set (map :id docs1))
                ids2 (set (map :id docs2))

                ]
            (doall (map (partial spu/index-add client resource-index1) docs1))
            (doall (map (partial spu/index-add client resource-index2) docs2))

            (spu/index-refresh client resource-index1)
            (spu/index-refresh client resource-index2)

            (doseq [[search-url nb ids] (map (fn [s n i] [s n i] ) resource-search-urls [n1 n2] [ids1 ids2])]
            (let [ch (spandex/scroll-chan client
                                          {:url  search-url
                                           :body {:query {:match_all {}}}})
                  db-ids (loop [existing-ids []]
                           (if-let [resp (<!! ch)]
                             (let [ids (->> (-> resp :body :hits :hits)
                                            (map :_source)
                                            (map :id))]
                               (recur (concat existing-ids ids)))
                             existing-ids))]
              (is (= nb (count ids)))
              (is (= nb (count db-ids)))
              (is (= ids (set db-ids)))))


            (is (= [[n1 n1 n1][n2 n2 n2]] (map <!! (t/meter-resources (:hosts rest-map) resource-search-urls metering-action))))

            (spu/index-refresh client metering-index)

            (let [ch (spandex/scroll-chan client
                                          {:url  metering-search-url
                                           :body {:query {:match_all {}}}})
                  db-ids (loop [existing-ids []]
                           (if-let [resp (<!! ch)]
                             (let [ids (->> (-> resp :body :hits :hits)
                                            (map (juxt #(str (:_type %) "/" (:_id %)) #(-> % :_source :id))))]
                               (recur (concat existing-ids ids)))
                             existing-ids))]
              (is (= (+ n1 n2) (count db-ids)))

              ;; check consistency between the CIMI resourceID and the Elasticsearch :_id and :_type keys.
              (is (apply = (first db-ids)))
              (is (every? #(apply = %) db-ids))))

          (finally
            (try
              (spu/index-delete client resource-index1)
              (catch Exception _))
            (try
              (spu/index-delete client metering-index)
              (catch Exception _))))))))
