(ns com.sixsq.slipstream.ssclj.resources.deployment-model-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.deployment-model :as dm]
    [com.sixsq.slipstream.ssclj.resources.deployment-model-template :as dmt]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.module-lifecycle-test :as module-test]
    [peridot.core :refer :all]))


(use-fixtures :each ltu/with-test-server-fixture)


(def collection-uri (str p/service-context (u/de-camelcase dm/resource-name)))


(deftest lifecycle

  (let [href (str dmt/resource-url "/standard")
        template-url (str p/service-context dmt/resource-url "/standard")

        session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-user (header session-anon authn-info-header "jane USER")
        session-admin (header session-anon authn-info-header "root ADMIN")

        module-uri (-> session-user
                       (request module-test/base-uri
                                :request-method :post
                                :body (json/write-str (assoc module-test/valid-entry
                                                        :content module-test/valid-image)))
                       (ltu/body->edn)
                       (ltu/is-status 201)
                       (ltu/location))

        resp (-> session-admin
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))

        template (get-in resp [:response :body])
        valid-create {:deploymentModelTemplate (ltu/strip-unwanted-attrs (merge template {:module {:href module-uri}}))}
        href-create (assoc-in valid-create [:deploymentModelTemplate :href] href)
        invalid-create (assoc-in valid-create [:deploymentModelTemplate :invalid] "BAD")]

    ;; anonymous create should fail
    (-> session-anon
        (request collection-uri
                 :request-method :post
                 :body (json/write-str valid-create))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; admin create with invalid template fails
    (-> session-admin
        (request collection-uri
                 :request-method :post
                 :body (json/write-str invalid-create))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; full connector lifecycle as user should work
    (let [uri (-> session-user
                  (request collection-uri
                           :request-method :post
                           :body (json/write-str valid-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          resource-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin get succeeds
      (-> session-admin
          (request resource-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; user get succeeds
      (-> session-user
          (request resource-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; user update works
      (-> session-user
          (request resource-uri
                   :request-method :put
                   :body (json/write-str {}))
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; user query succeeds
      (-> session-user
          (request collection-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; anonymous query fails
      (-> session-anon
          (request collection-uri)
          (ltu/body->edn)
          (ltu/is-status 403))

      ;; user query succeeds
      (let [entries (-> session-user
                        (request collection-uri)
                        (ltu/body->edn)
                        (ltu/is-status 200)
                        (ltu/is-resource-uri dm/collection-uri)
                        (ltu/is-count #(= 1 %))
                        (ltu/entries dm/resource-tag))]
        (is ((set (map :id entries)) uri))

        ;; verify that all entries are accessible
        (let [pair-fn (juxt :id #(str p/service-context (:id %)))
              pairs (map pair-fn entries)]
          (doseq [[id entry-uri] pairs]
            (-> session-user
                (request entry-uri)
                (ltu/body->edn)
                (ltu/is-operation-present "delete")
                (ltu/is-operation-present "edit")
                (ltu/is-operation-absent "activate")
                (ltu/is-operation-absent "quarantine")
                (ltu/is-status 200)
                (ltu/is-id id)))))

      ;; user delete succeeds
      (-> session-user
          (request resource-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> session-admin
          (request resource-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))

    ;; abbreviated lifecycle using href to template instead of copy
    (let [uri (-> session-user
                  (request collection-uri
                           :request-method :post
                           :body (json/write-str href-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; user delete succeeds
      (-> session-user
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
  (let [resource-uri (str p/service-context (u/new-resource-id dm/resource-name))]
    (ltu/verify-405-status [[collection-uri :options]
                            [collection-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))
