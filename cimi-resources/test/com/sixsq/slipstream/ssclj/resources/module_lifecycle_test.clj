(ns com.sixsq.slipstream.ssclj.resources.module-lifecycle-test
  (:require
    [clojure.test :refer [deftest is are use-fixtures]]
    [peridot.core :refer [session request header content-type]]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.module :as t]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase t/resource-url)))

(def timestamp "1964-08-25T10:00:00.0Z")


(def valid-module {:id          "module/my-uuid-string"
                   :resourceURI t/resource-uri
                   :created     timestamp
                   :updated     timestamp
                   :name        "short-name"
                   :description "short description",
                   :properties  {:a "one", :b "two"}

                   :parent      "my/module/parent"
                   :path        "my/module/parent/short-name"
                   :type        "project"

                   :versions    [{:href "module-version/my-uuid-version-1"}
                                 {:href "module-version/my-uuid-version-2"}]})


(deftest lifecycle
  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-admin (header session-anon authn-info-header "super ADMIN USER ANON")
        session-user (header session-anon authn-info-header "jane USER ANON")]

    ;; admin query succeeds but is empty
    (-> session-admin
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; user query succeeds but is empty
    (-> session-user
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; anon query fails
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; anon create must fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-module))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; check module creation
    (let [admin-uri (-> session-admin
                        (request base-uri
                                 :request-method :post
                                 :body (json/write-str valid-module))
                        (ltu/body->edn)
                        (ltu/is-status 201)
                        (ltu/location))
          admin-abs-uri (str p/service-context (u/de-camelcase admin-uri))

          user-uri (-> session-user
                       (request base-uri
                                :request-method :post
                                :body (json/write-str valid-module))
                       (ltu/body->edn)
                       (ltu/is-status 201)
                       (ltu/location))
          user-abs-uri (str p/service-context (u/de-camelcase user-uri))]

      ;; admin should see 2 module resources
      (-> session-admin
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri t/collection-uri)
          (ltu/is-count 2))

      ;; user should see only 1 module resource
      (-> session-user
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri t/collection-uri)
          (ltu/is-count 1))

      ;; verify contents of admin module
      (let [module (-> session-admin
                       (request admin-abs-uri)
                       (ltu/body->edn)
                       (ltu/is-status 200)
                       (ltu/is-operation-present "edit")
                       (ltu/is-operation-present "delete")
                       :response
                       :body)]
        (is (= "my/module/parent" (:parent module))))

      ;; verify contents of user module
      (let [module (-> session-user
                       (request user-abs-uri)
                       (ltu/body->edn)
                       (ltu/is-status 200)
                       (ltu/is-operation-present "edit")
                       (ltu/is-operation-present "delete")
                       :response
                       :body)]
        (is (= "my/module/parent" (:parent module))))


      ;; admin can delete the module
      (-> session-admin
          (request admin-abs-uri :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; user can delete the module
      (-> session-user
          (request user-abs-uri :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id t/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :put]
                            [resource-uri :options]
                            [resource-uri :post]])))
