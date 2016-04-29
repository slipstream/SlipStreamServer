(ns com.sixsq.slipstream.ssclj.resources.usage-event-test
  (:require
    [clojure.test :refer :all]
    [korma.core :as kc]

    [peridot.core :refer :all]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]

    [com.sixsq.slipstream.ssclj.api.acl :as acl]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]
    [com.sixsq.slipstream.ssclj.resources.usage-event :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]))

(defn reset-events
  [f]
  (rc/-init)
  (dbdb/init-db)
  (kc/delete dbdb/resources)
  (kc/delete acl/acl)
  (f))

(use-fixtures :each reset-events)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn make-ring-app [resource-routes]
  (db/set-impl! (esb/get-instance))
  (-> resource-routes
      wrap-exceptions
      wrap-base-uri
      wrap-params
      wrap-authn-info-header
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})))

(defn ring-app []
  (make-ring-app (t/concat-routes routes/final-routes)))

(def open-usage-event
  {:acl                 {:owner {:type "USER" :principal "joe"}
                         :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}
   :cloud-vm-instanceid "exo:one"
   :user                "joe"
   :cloud               "exo"
   :start-timestamp     "2015-05-04T15:32:22.853Z"
   :metrics             [{:name "vm" :value "1.0"}
                         {:name "disk" :value "1260.0"}]})

(def close-usage-event (assoc open-usage-event :end-timestamp "2015-05-04T15:45:22.853Z"))
(def open-usage-event-other-cloud-vm-instance-id (assoc open-usage-event :cloud-vm-instanceid "exo:another"))
(def open-usage-event-other-metric (assoc open-usage-event :metrics [{:name "cpu" :value "2.0"}]))
(def reopen-usage-event-new-value (assoc open-usage-event :start-timestamp "2015-05-04T16:07:22.853Z"
                                                          :metrics [{:name "vm" :value "2.0"}]))

(deftest post-open-usage-event-should-open-records
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "joe")
      (request "/api/usage-event"
               :request-method :post
               :body (json/write-str open-usage-event))
      t/body->json
      (t/is-status 201))
  (let [[ur1 ur2] (sort-by :metric-name
                           (-> (session (ring-app))
                               (content-type "application/json")
                               (header authn-info-header "joe")
                               (request "/api/usage-record")
                               t/body->json
                               (t/is-count #(= 2 %))
                               (get-in [:response :body :usage-records])))]

    (is (= (-> open-usage-event (dissoc :metrics) (assoc :metric-name "disk" :metric-value "1260.0" :end-timestamp nil))
           (-> ur1 (dissoc :id))))
    (is (= (-> open-usage-event (dissoc :metrics) (assoc :metric-name "vm" :metric-value "1.0" :end-timestamp nil))
           (-> ur2 (dissoc :id))))))

(defn all-records
  [state]
  (-> (request state "/api/usage-record")
      t/body->json
      (get-in [:response :body :usage-records])))

(defn end-timestamps-of-all-records
  [state]
  (->> (all-records state)
       (map :end-timestamp)))

(deftest post-close-usage-event-should-update-records
  (let [state (-> (session (ring-app))
                  (content-type "application/json")
                  (header authn-info-header "joe"))]
    (request state "/api/usage-event"
             :request-method :post
             :body (json/write-str open-usage-event))
    (request state "/api/usage-event"
             :request-method :post
             :body (json/write-str open-usage-event-other-cloud-vm-instance-id))
    (request state "/api/usage-event"
             :request-method :post
             :body (json/write-str open-usage-event-other-metric))

    (is (every? nil? (end-timestamps-of-all-records state)))

    (request state "/api/usage-event"
             :request-method :post
             :body (json/write-str close-usage-event))

    (let [end-timestamps-records-after-close (end-timestamps-of-all-records state)]
      (is (= 5 (count end-timestamps-records-after-close)))
      (is (= ["2015-05-04T15:45:22.853Z" "2015-05-04T15:45:22.853Z"]
             (->> (all-records state)
                  (filter #(= "exo:one" (:cloud-vm-instanceid %)))
                  (filter #(#{"vm" "disk"} (:metric-name %)))
                  (map :end-timestamp))))
      (is (= [nil nil]
             (->> (all-records state)
                  (filter #(= "exo:another" (:cloud-vm-instanceid %)))
                  (map :end-timestamp))))
      (is (= [nil]
             (->> (all-records state)
                  (filter #(= "cpu" (:metric-name %)))
                  (map :end-timestamp)))))))

(deftest post-close-usage-event-when-not-open-should-do-nothing
  (let [state
        (-> (session (ring-app))
            (content-type "application/json")
            (header authn-info-header "joe"))]
    (request state "/api/usage-event"
             :request-method :post
             :body (json/write-str close-usage-event))
    (is (empty? (all-records state)))))

(deftest post-close-usage-event-when-already-closed-should-do-nothing
  (let [state
        (-> (session (ring-app))
            (content-type "application/json")
            (header authn-info-header "joe"))]
    (request state "/api/usage-event" :request-method :post :body (json/write-str open-usage-event))
    (is (= 2 (count (all-records state))))
    (request state "/api/usage-event" :request-method :post :body (json/write-str close-usage-event))
    (let [urs (all-records state)]
      (is (= 2 (count urs)))
      (request state "/api/usage-event" :request-method :post :body (json/write-str close-usage-event))
      (is (= urs (all-records state))))))

(deftest post-open-usage-event-when-already-open-should-close-restart
  (let [state
        (-> (session (ring-app))
            (content-type "application/json")
            (header authn-info-header "joe"))]
    (request state "/api/usage-event" :request-method :post :body (json/write-str open-usage-event))
    (is (= 2 (count (all-records state))))
    (request state "/api/usage-event" :request-method :post :body (json/write-str reopen-usage-event-new-value))
    (let [urs (all-records state)]
      (is (= 3 (count urs)))
      (is (= [["2015-05-04T15:32:22.853Z" "2015-05-04T16:07:22.853Z"]
              ["2015-05-04T16:07:22.853Z" nil]]
             (->> urs
                  (filter #(= "vm" (:metric-name %)))
                  (sort-by :start-timestamp)
                  (map (juxt :start-timestamp :end-timestamp))))))))

(deftest post-open-usage-event-when-already-close-should-restart
  (let [state
        (-> (session (ring-app))
            (content-type "application/json")
            (header authn-info-header "joe"))]
    (request state "/api/usage-event" :request-method :post :body (json/write-str open-usage-event))
    (request state "/api/usage-event" :request-method :post :body (json/write-str close-usage-event))
    (is (= 2 (count (all-records state))))
    (request state "/api/usage-event" :request-method :post :body (json/write-str reopen-usage-event-new-value))
    (is (= 3 (count (all-records state))))
    (is (= [["2015-05-04T15:32:22.853Z" "2015-05-04T15:45:22.853Z"]
            ["2015-05-04T16:07:22.853Z" nil]]
           (->> (all-records state)
                (filter #(= "vm" (:metric-name %)))
                (sort-by :start-timestamp)
                (map (juxt :start-timestamp :end-timestamp)))))))

