(ns com.sixsq.slipstream.ssclj.resources.connector-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.connector :refer :all]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.connector-template-alpha-example :as example]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

;; initialize must to called to pull in ConnectorTemplate test examples
(dyn/initialize)

(deftest lifecycle

  (let [href (str ct/resource-url "/" example/cloud-service-type)
        template-url (str p/service-context ct/resource-url "/" example/cloud-service-type)
        resp (-> (session (ltu/ring-app))
                 (content-type "application/json")
                 (header authn-info-header "root ADMIN")
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))
        template (get-in resp [:response :body])
        valid-create {:connectorTemplate (ltu/strip-unwanted-attrs (merge template {:alphaKey     2001
                                                                                    :instanceName "alpha-omega"}))}
        href-create {:connectorTemplate {:href         href
                                         :alphaKey     3001
                                         :instanceName "alpha-omega"}}
        invalid-create (assoc-in valid-create [:connectorTemplate :invalid] "BAD")]

    ;; anonymous create should fail
    (-> (session (ltu/ring-app))
        (content-type "application/json")
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-create))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; user create should also fail
    (-> (session (ltu/ring-app))
        (content-type "application/json")
        (header authn-info-header "jane USER")
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-create))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; admin create with invalid template fails
    (-> (session (ltu/ring-app))
        (content-type "application/json")
        (header authn-info-header "root ADMIN")
        (request base-uri
                 :request-method :post
                 :body (json/write-str invalid-create))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; full connector lifecycle as administrator should work
    (let [uri (-> (session (ltu/ring-app))
                  (content-type "application/json")
                  (header authn-info-header "root ADMIN")
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin get succeeds
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; anonymous query fails
      (-> (session (ltu/ring-app))
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 403))

      ;; admin query succeeds
      (let [entries (-> (session (ltu/ring-app))
                        (content-type "application/json")
                        (header authn-info-header "root ADMIN")
                        (request base-uri)
                        (ltu/body->edn)
                        (ltu/is-status 200)
                        (ltu/is-resource-uri collection-uri)
                        (ltu/is-count #(= 1 %))
                        (ltu/entries resource-tag))]
        (is ((set (map :id entries)) uri))

        ;; verify that all entries are accessible
        (let [pair-fn (juxt :id #(str p/service-context (:id %)))
              pairs (map pair-fn entries)]
          (doseq [[id entry-uri] pairs]
            (-> (session (ltu/ring-app))
                (header authn-info-header "root ADMIN")
                (request entry-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-id id)))))

      ;; admin delete succeeds
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))

    ;; abbreviated lifecycle using href to template instead of copy
    (let [uri (-> (session (ltu/ring-app))
                  (content-type "application/json")
                  (header authn-info-header "root ADMIN")
                  (request base-uri
                           :request-method :post
                           :body (json/write-str href-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin delete succeeds
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (-> (session (ltu/ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
