(ns com.sixsq.slipstream.ssclj.resources.quota-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.quota :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.quota :as quota]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes routes/final-routes)))

(def valid-entry
  (let [timestamp "1964-08-25T10:00:00.0Z"
        valid-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "jane"
                            :type      "USER"
                            :right     "VIEW"}]}]
    {:id          (str quota/resource-url "/test-quota")
     :name        "Test Quota"
     :description "An example quota with a value."
     :resourceURI quota/resource-uri
     :created     timestamp
     :updated     timestamp
     :acl         valid-acl

     :collection  "virtualMachines"
     :selection   "organization='cern'"
     :aggregation "count:id"
     :limit       100}))

(def invalid-entry
  (merge valid-entry {:other "BAD"}))

(deftest lifecycle

  (let [session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "super ADMIN USER ANON"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json"))]

    ;; anonymous create should fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-entry))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; anonymous query should also fail
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))

    ; adding the quota and reading it as user should succeed
    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-entry))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; user view: OK
      (-> session-user
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; admin view: OK
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; user edit: FAIL
      (let [old-quota (-> session-admin
                          (request abs-uri)
                          (ltu/body->edn)
                          (ltu/is-status 200)
                          :response
                          :body)
            old-limit (:limit old-quota)
            new-limit (* 2 old-limit)
            new-quota (assoc old-quota :limit new-limit)]

        (-> session-admin
            (request abs-uri
                     :request-method :put
                     :body (json/write-str new-quota))
            (ltu/is-status 200))

        (let [reread-limit (-> session-admin
                               (request abs-uri)
                               (ltu/body->edn)
                               (ltu/is-status 200)
                               :response
                               :body
                               :limit)]
          (is (not= old-limit reread-limit))
          (is (= new-limit reread-limit))))

      ;; user delete: FAIL
      (-> session-user
          (request abs-uri :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 403))

      ;; admin delete: OK
      (-> session-admin
          (request abs-uri :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (do
          (-> (session (ring-app))
              (request uri
                       :request-method method
                       :body (json/write-str {:dummy "value"}))
              (ltu/is-status 405)))))))
