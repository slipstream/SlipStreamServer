(ns sixsq.slipstream.metering.metering-live-test
  (:require
    [clojure.test :refer [deftest is are]]
    [clojure.core.async :refer [<!!]]
    [qbits.spandex :as spandex]
    [sixsq.slipstream.metering.metering :as t]
    [sixsq.slipstream.metering.utils :as utils]
    [sixsq.slipstream.metering.spandex-utils :as spu]))

(defn random-doc
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


(deftest check-empty-bulk-insert
  (let [hosts ["http://localhost:9200"]]
    (with-open [client (spandex/client {:hosts hosts})]
      (let [ch (t/bulk-insert client [])]
        (is (nil? (<!! ch)))))))

(deftest lifecycle
  (let [resource-index (utils/random-uuid)
        resource-type "virtual-machine"
        resource-search-url (t/search-url resource-index resource-type)
        metering-index (utils/random-uuid)
        metering-type "metering"
        metering-action (t/index-action metering-index metering-type)
        rest-map (spu/provide-mock-rest-client)]
    (with-open [client (:client rest-map)]
      (when (spu/cluster-ready? client)
        (spu/index-create client resource-index)
        (try
          (let [n 199
                docs (repeatedly n (partial random-doc resource-type))
                ids (set (map :id docs))]
            (doall (map (partial spu/index-add client resource-index) docs))

            (spu/index-refresh client resource-index)

            (let [ch (spandex/scroll-chan client
                                          {:url  resource-search-url
                                           :body {:query {:match_all {}}}})
                  db-ids (loop [existing-ids []]
                           (if-let [resp (<!! ch)]
                             (let [ids (->> (-> resp :body :hits :hits)
                                            (map :_source)
                                            (map :id))]
                               (recur (concat existing-ids ids)))
                             existing-ids))]
              (is (= n (count ids)))
              (is (= n (count db-ids)))
              (is (= ids (set db-ids)))
              (is (= [n n n] (<!! (t/meter-resources (:hosts rest-map) resource-search-url metering-action))))))
          (finally
            (spu/index-delete client resource-index)))))))
