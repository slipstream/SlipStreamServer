(ns com.sixsq.slipstream.ssclj.resources.deployment-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.deployment :as deployment]
    [com.sixsq.slipstream.ssclj.resources.deployment-template :as deployment-template]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.module-lifecycle-test :as module-test]
    [peridot.core :refer :all]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context deployment/resource-url))

(def deployment-template-collection-uri (str p/service-context (u/de-camelcase deployment-template/resource-name)))

(deftest lifecycle

  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-admin (header session-anon authn-info-header "super ADMIN USER ANON")
        session-user (header session-anon authn-info-header "jane USER ANON")
        module-uri (-> session-user
                       (request module-test/base-uri
                                :request-method :post
                                :body (json/write-str (assoc module-test/valid-entry
                                                        :content module-test/valid-image)))
                       (ltu/body->edn)
                       (ltu/is-status 201)
                       (ltu/location))
        valid-deployment-template-create {:module {:href module-uri}}
        deployment-template-uri (-> session-user
                                    (request deployment-template-collection-uri
                                             :request-method :post
                                             :body (json/write-str valid-deployment-template-create))
                                    (ltu/body->edn)
                                    (ltu/is-status 201)
                                    (ltu/location))
        valid-deployment {:deploymentTemplate {:href deployment-template-uri}}]

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
    (let [uri (-> session-user
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-deployment))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; user query: ok
      (-> session-user
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri deployment/collection-uri)
          (ltu/is-count #(= 1 %))
          (ltu/entries deployment/resource-tag))

      ;; admin query: ok
      (-> session-admin
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri deployment/collection-uri)
          (ltu/is-count #(= 1 %))
          (ltu/entries deployment/resource-tag))

      ;; user view: OK
      (let [
            ;; CREATED state on creation
            start-op (-> session-user
                         (request abs-uri)
                         (ltu/body->edn)
                         (ltu/is-status 200)
                         (ltu/is-operation-present "edit")
                         (ltu/is-operation-present "delete")
                         (ltu/is-operation-present (:start c/action-uri))
                         (ltu/is-operation-absent (:stop c/action-uri))
                         (ltu/get-op "start"))
            abs-start-uri (str p/service-context (u/de-camelcase start-op))

            start-resp (-> session-user
                           (request abs-start-uri
                                    :request-method :post)
                           (ltu/body->edn)
                           (ltu/is-status 202)
                           :response
                           :body)

            ;; STARTING state after start action
            stop-op (-> session-user
                        (request abs-uri)
                        (ltu/body->edn)
                        (ltu/is-status 200)
                        (ltu/is-operation-present "edit")
                        (ltu/is-operation-absent "delete")
                        (ltu/is-operation-absent (:start c/action-uri))
                        (ltu/is-operation-present (:stop c/action-uri))
                        (ltu/get-op "stop"))
            abs-stop-uri (str p/service-context (u/de-camelcase stop-op))

            stop-resp (-> session-user
                          (request abs-stop-uri
                                   :request-method :post)
                          (ltu/body->edn)
                          (ltu/is-status 202)
                          :response
                          :body)
            ]

        ;; STOPPING state after stop action
        (-> session-user
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-operation-present "edit")
            (ltu/is-operation-absent "delete")
            (ltu/is-operation-absent (:start c/action-uri))
            (ltu/is-operation-absent (:stop c/action-uri))
            (ltu/is-key-value :state "STOPPING"))

        ;; user delete: FAIL
        (-> session-user
            (request abs-uri :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 409))))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id deployment/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))
