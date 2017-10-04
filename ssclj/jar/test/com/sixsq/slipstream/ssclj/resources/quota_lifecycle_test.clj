(ns com.sixsq.slipstream.ssclj.resources.quota-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.quota :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.quota.utils-test :as quota-test-utils]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.quota :as quota]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes routes/final-routes)))

(def quota-jane (quota-test-utils/make-quota "jane" "count:id" 100))

(deftest lifecycle

  (let [n-vm 300

        session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "super ADMIN USER ANON"))
        session-jane (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-tarzan (-> (session (ring-app))
                           (content-type "application/json")
                           (header authn-info-header "tarzan USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json"))]

    ;; create some virtual machine resources
    (let [freq (quota-test-utils/create-virtual-machines n-vm)]

      ;; anonymous create should fail
      (-> session-anon
          (request base-uri
                   :request-method :post
                   :body (json/write-str quota-jane))
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
                             :body (json/write-str quota-jane))
                    (ltu/body->edn)
                    (ltu/is-status 201)
                    (ltu/location))
            abs-uri (str p/service-context (u/de-camelcase uri))]

        ;; admin query: ok
        (-> session-admin
            (request base-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-resource-uri collection-uri)
            (ltu/is-count #(= 1 %))
            (ltu/entries resource-tag))

        ;; user query: ok
        (-> session-jane
            (request base-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-resource-uri collection-uri)
            (ltu/is-count #(= 1 %))
            (ltu/entries resource-tag))

        ;; other user query: ok, but empty
        (-> session-tarzan
            (request base-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-resource-uri collection-uri)
            (ltu/is-count zero?))

        ;; user view: OK
        (let [collect-op (-> session-jane
                             (request abs-uri)
                             (ltu/body->edn)
                             (ltu/is-status 200)
                             (ltu/is-operation-absent "edit")
                             (ltu/is-operation-absent "delete")
                             (ltu/is-operation-present (:collect c/action-uri))
                             (ltu/get-op "collect"))
              abs-collect-uri (str p/service-context (u/de-camelcase collect-op))

              collect-resp (-> session-jane
                               (request abs-collect-uri
                                        :request-method :post)
                               (ltu/body->edn)
                               (ltu/is-status 200)
                               :response
                               :body)]

          ;; check that the collect action returns the correct response
          (is (= n-vm (:current-all collect-resp)))
          (is (= (get freq "jane") (:current-user collect-resp)))
          (is (= 100 (:limit collect-resp))))

        ;; other user view: FAIL
        (-> session-tarzan
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 403))

        ;; admin view: OK
        (let [collect-op (-> session-admin
                             (request abs-uri)
                             (ltu/body->edn)
                             (ltu/is-status 200)
                             (ltu/is-operation-present "edit")
                             (ltu/is-operation-present "delete")
                             (ltu/is-operation-present (:collect c/action-uri))
                             (ltu/get-op "collect"))
              abs-collect-uri (str p/service-context (u/de-camelcase collect-op))

              collect-resp (-> session-admin
                               (request abs-collect-uri
                                        :request-method :post)
                               (ltu/body->edn)
                               (ltu/is-status 200)
                               :response
                               :body)]

          ;; check that the collect action returns the correct response
          (is (= n-vm (:current-all collect-resp)))
          (is (= n-vm (:current-user collect-resp)))
          (is (= 100 (:limit collect-resp))))

        ;; admin edit: OK
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
        (-> session-jane
            (request abs-uri :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 403))

        ;; admin delete: OK
        (-> session-admin
            (request abs-uri :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 200))))))

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
