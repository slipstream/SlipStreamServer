(ns com.sixsq.slipstream.ssclj.resources.deployment-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.deployment :as d]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-test :as dt]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context d/resource-url))

(deftest lifecycle

  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-admin (header session-anon authn-info-header "super ADMIN USER ANON")
        session-jane (header session-anon authn-info-header "jane USER ANON")
        valid-deployment (dissoc dt/valid-deployment :acl)]

    ;; anonymous create should fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-deployment))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; anonymous query should also fail
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))

    ; adding the deployment and reading it as user should succeed
    (let [uri (-> session-jane
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-deployment))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; user query: ok
      (-> session-jane
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri d/collection-uri)
          (ltu/is-count #(= 1 %))
          (ltu/entries d/resource-tag))

      ;; admin query: ok
      (-> session-admin
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri d/collection-uri)
          (ltu/is-count #(= 1 %))
          (ltu/entries d/resource-tag))

      ;; user view: OK
      (let [start-op (-> session-jane
                         (request abs-uri)
                         (ltu/body->edn)
                         (ltu/is-status 200)
                         (ltu/is-operation-present "edit")
                         (ltu/is-operation-present "delete")
                         (ltu/is-operation-present (:start c/action-uri))
                         (ltu/is-operation-present (:stop c/action-uri))
                         (ltu/get-op "start"))
            abs-start-uri (str p/service-context (u/de-camelcase start-op))

            start-resp (-> session-jane
                           (request abs-start-uri
                                    :request-method :post)
                           (ltu/body->edn)
                           (ltu/is-status 202)
                           :response
                           :body)]

        ;; user delete: FAIL
        (-> session-jane
            (request abs-uri :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 200))))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id d/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))
