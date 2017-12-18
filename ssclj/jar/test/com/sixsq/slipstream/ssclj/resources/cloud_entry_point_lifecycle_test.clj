(ns com.sixsq.slipstream.ssclj.resources.cloud-entry-point-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.cloud-entry-point :as t]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase t/resource-name)))


(deftest lifecycle

  ;; ensure that database implementation has been bound
  (ltu/ring-app)

  ;; initialize the root resource
  (let [response (t/add)]
    (is (= 201 (:status response)))
    (is (= (str t/resource-url) (-> response
                                    :headers
                                    (get "Location"))))
    (is (:body response))

    (let [session-anon (-> (ltu/ring-app)
                           session
                           (content-type "application/json"))
          session-admin (header session-anon authn-info-header "root ADMIN")
          session-user (header session-anon authn-info-header "jane-updater")]

      ; retrieve root resource (anonymously should work)
      (-> session-anon
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri t/resource-uri)
          (ltu/is-operation-absent "edit")
          (ltu/is-operation-absent "delete"))

      ;; retrieve root resource (root should have edit rights)
      (-> session-admin
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri t/resource-uri)
          (ltu/is-operation-present "edit")
          (ltu/is-operation-absent "delete"))

      ;; updating root resource as user should fail
      (-> session-user
          (request base-uri
                   :request-method :put
                   :body (json/write-str {:name "dummy"}))
          (ltu/body->edn)
          (ltu/is-status 403))

      ;; update the entry, verify updated doc is returned
      ;; must be done as administrator
      (-> session-admin
          (request base-uri
                   :request-method :put
                   :body (json/write-str {:name "dummy"}))
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri t/resource-uri)
          (ltu/is-operation-present "edit")
          (ltu/is-key-value :name "dummy"))

      ;; verify that subsequent reads find the right data
      (-> session-anon
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri t/resource-uri)
          (ltu/is-operation-absent "edit")
          (ltu/is-key-value :name "dummy")))))


(deftest bad-methods
  (ltu/verify-405-status [[base-uri :options]
                          [base-uri :delete]
                          [base-uri :post]]))



