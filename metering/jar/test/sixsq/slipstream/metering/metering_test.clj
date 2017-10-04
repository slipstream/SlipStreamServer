(ns sixsq.slipstream.metering.metering-test
  (:require
    [clojure.test :refer [deftest is are]]
    [sixsq.slipstream.metering.metering :as t]
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]))


(deftest check-es-hosts
  (is (= ["http://a:1234"] (t/es-hosts "a" 1234)))
  (is (= ["http://a:1234"] (t/es-hosts "a" "1234"))))


(deftest check-index-action
  (is (= {:index {:_index "index", :_type "type"}}
         (t/index-action "index" "type"))))


(deftest check-search-url
  (is (= "index/type/_search" (t/search-url "index" "type"))))


(deftest check-process-options
  (let [{:keys [hosts
                resource-search-url
                metering-action
                metering-period-minutes]
         :as   options}
        (t/process-options {})]

    (is (= 4 (count options)))
    (is (= ["http://127.0.0.1:9200"] hosts))
    (is (= "resources-index/virtual-machine/_search" resource-search-url))
    (is (= "resources-index" (-> metering-action :index :_index)))
    (is (= "metering" (-> metering-action :index :_type)))
    (is (= 1 metering-period-minutes)))

  (is (= ["http://elasticsearch:1234"] (:hosts (t/process-options {:es-host "elasticsearch"
                                                                 :es-port 1234}))))
  (is (= 2 (:metering-period-minutes (t/process-options {:metering-period-minutes 2}))))
  (is (= "alpha/vms/_search" (:resource-search-url (t/process-options {:resources-index "alpha"
                                                                       :resources-type  "vms"}))))

  (let [{:keys [metering-action]}
        (t/process-options {:metering-index "metering"
                            :metering-type  "vms"})]
    (is (= "metering" (-> metering-action :index :_index)))
    (is (= "vms" (-> metering-action :index :_type)))))


(deftest check-assoc-snapshot-time
  (let [ts "2017-09-10T16:50:00.360Z"]
    (is (= {:snapshot-time ts} (t/assoc-snapshot-time ts {})))
    (is (= {:snapshot-time ts} (t/assoc-snapshot-time ts {:snapshot-time "BAD!"})))))


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
    (let [actions (t/create-actions "timestamp" "action" search-result-doc)]
      (is (pos? (count actions)))
      (is (->> actions
               (map first)
               (every? #(= "action" %))))
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
