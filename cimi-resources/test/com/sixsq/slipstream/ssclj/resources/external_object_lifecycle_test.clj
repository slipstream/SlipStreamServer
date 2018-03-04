(ns com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.external-object :refer :all]
    [com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test-utils :as eoltu]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-alpha-example :as example]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [clojure.data.json :as json]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))


(deftest lifecycle
  (eoltu/lifecycle (str p/service-context eot/resource-url "/" example/objectType)
                   {:alphaKey 3001}))


(deftest check-upload-and-download-operations
  (eoltu/upload-and-download-operations (str p/service-context eot/resource-url "/" example/objectType)
                                        {:alphaKey 2002}))


(deftest lifecycle-href
  (let [href (str eot/resource-url "/" example/objectType)

        session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-admin (header session-anon authn-info-header "root ADMIN")

        href-create {:externalObjectTemplate {:href     href
                                              :alphaKey 3001}}]

    ;; abbreviated lifecycle using href to template instead of copy
    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str href-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin delete succeeds
      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))


