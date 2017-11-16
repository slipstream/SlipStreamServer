(ns com.sixsq.slipstream.ssclj.resources.user-params-exec-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]

    [com.sixsq.slipstream.ssclj.resources.user-params :as up]
    [com.sixsq.slipstream.ssclj.resources.user-params-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.user-params-template-exec :as exec]

    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [clojure.spec.alpha :as s]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase up/resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in SessionTemplate test examples
(dyn/initialize)

(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(deftest lifecycle
  (let [href              (str ct/resource-url "/" exec/params-type)
        template-url      (str p/service-context ct/resource-url "/" exec/params-type)
        session-admin     (-> (session (ring-app))
                              (content-type "application/json")
                              (header authn-info-header "root ADMIN"))
        session-user      (-> (session (ring-app))
                              (content-type "application/json")
                              (header authn-info-header "jane USER ANON"))
        session-user2     (-> (session (ring-app))
                              (content-type "application/json")
                              (header authn-info-header "john USER ANON"))
        session-anon      (-> (session (ring-app))
                              (content-type "application/json")
                              (header authn-info-header "unknown ANON"))
        template          (-> session-admin
                              (request template-url)
                              (ltu/body->edn)
                              (ltu/is-status 200)
                              (get-in [:response :body]))
        create-from-templ {:userParamTemplate (-> template
                                                  strip-unwanted-attrs
                                                  (merge {:defaultCloudService "foo-bar-baz"
                                                          :timeout             30}))}]

    ;; Query.
    ;; anonymous user collection query should fail
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; admin user collection query should succeed but be empty (no user
    ;; params created yet)
    (-> session-admin
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; Create.
    ;; only one document per parameter type is allowed per user
    (let [uri        (-> session-user
                         (request base-uri
                                  :request-method :post
                                  :body (json/write-str create-from-templ))
                         (ltu/body->edn)
                         (ltu/is-status 201)
                         (ltu/location))
          u1-abs-uri (str p/service-context (u/de-camelcase uri))]
      (-> session-user
          (request u1-abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; second create for the same user should fail with 409 Conflict
      (-> session-user
          (request base-uri
                   :request-method :post
                   :body (json/write-str create-from-templ))
          (ltu/body->edn)
          (ltu/is-status 409))

      ;; another user can create ...
      (-> session-user2
          (request base-uri
                   :request-method :post
                   :body (json/write-str create-from-templ))
          (ltu/body->edn)
          (ltu/is-status 201))

      ;; but only once
      (-> session-user2
          (request base-uri
                   :request-method :post
                   :body (json/write-str create-from-templ))
          (ltu/body->edn)
          (ltu/is-status 409))

      ;; Edit.
      ;; user can edit the document
      (let [resource       (-> session-admin
                               (request u1-abs-uri)
                               (ltu/body->edn)
                               :response
                               :body)
            time-out       (+ (:timeout resource) 10)
            verbosity      (+ (:verbosityLevel resource) 1)
            timeout-json   (json/write-str (assoc resource :timeout time-out))
            verbosity-json (json/write-str (assoc resource :verbosityLevel verbosity))]

        ;; anon user can NOT edit
        (-> session-anon
            (request u1-abs-uri
                     :request-method :put
                     :body timeout-json)
            (ltu/is-status 403))

        ;; another user can NOT edit
        (-> session-user2
            (request u1-abs-uri
                     :request-method :put
                     :body timeout-json)
            (ltu/is-status 403))

        ;; owner can edit
        (-> session-user
            (request u1-abs-uri
                     :request-method :put
                     :body timeout-json)
            (ltu/body->edn)
            (ltu/is-status 200))
        (is (= time-out (-> session-admin
                            (request u1-abs-uri)
                            (ltu/body->edn)
                            :response
                            :body
                            :timeout)))

        ;; super can edit
        (-> session-admin
            (request u1-abs-uri
                     :request-method :put
                     :body verbosity-json)
            (ltu/body->edn)
            (ltu/is-status 200))
        (is (= verbosity (-> session-admin
                             (request u1-abs-uri)
                             (ltu/body->edn)
                             :response
                             :body
                             :verbosityLevel)))

        ;; edit non-existent document - 404
        (-> session-user
            (request (str u1-abs-uri "-fake")
                     :request-method :put
                     :body verbosity-json)
            (ltu/body->edn)
            (ltu/is-status 404)))

      ;; Delete.
      ;; users can delete their user param documents
      (-> session-user
          (request u1-abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))
      (ltu/refresh-es-indices)
      (-> session-user
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-count 0))

      (let [resp    (-> session-user2
                        (request base-uri)
                        (ltu/body->edn)
                        (ltu/is-status 200)
                        (ltu/is-count 1))
            uri     (->> resp
                         :response
                         :body
                         :userParam
                         first
                         :id)
            abs-uri (str p/service-context (u/de-camelcase uri))]
        (-> session-user2
            (request abs-uri
                     :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 200))
        (ltu/refresh-es-indices)
        (-> session-user2
            (request base-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-count 0))))))
