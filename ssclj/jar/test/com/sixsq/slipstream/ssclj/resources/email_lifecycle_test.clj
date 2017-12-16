(ns com.sixsq.slipstream.ssclj.resources.email-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.email :as t]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase t/resource-url)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(def valid-acl {:owner {:principal "ADMIN",
                        :type      "ROLE"},
                :rules [{:principal "realm:accounting_manager",
                         :type      "ROLE",
                         :right     "VIEW"},
                        {:principal "test",
                         :type      "USER",
                         :right     "VIEW"},
                        {:principal "cern:cern",
                         :type      "ROLE",
                         :right     "VIEW"},
                        {:principal "cern:my-accounting-group",
                         :type      "ROLE",
                         :right     "VIEW"}]})

(deftest lifecycle
  (let [session-anon (-> (ring-app)
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
                 :body (json/write-str {:address "anon@example.com"}))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; check email creation
    (let [admin-uri (-> session-admin
                        (request base-uri
                                 :request-method :post
                                 :body (json/write-str {:address    "admin@example.com"
                                                        :validated? true}))
                        (ltu/body->edn)
                        (ltu/is-status 201)
                        (ltu/location))
          admin-abs-uri (str p/service-context (u/de-camelcase admin-uri))

          user-uri (-> session-user
                       (request base-uri
                                :request-method :post
                                :body (json/write-str {:address    "user@example.com"
                                                       :validated? true}))
                       (ltu/body->edn)
                       (ltu/is-status 201)
                       (ltu/location))
          user-abs-uri (str p/service-context (u/de-camelcase user-uri))]

      ;; admin should see 2 email resources
      (-> session-admin
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri t/collection-uri)
          (ltu/is-count 2))

      ;; user should see only 1
      (-> session-user
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri t/collection-uri)
          (ltu/is-count 1))

      ;; verify contents of admin email
      (let [email (-> session-admin
                      (request admin-abs-uri)
                      (ltu/body->edn)
                      (ltu/is-status 200)
                      (ltu/is-operation-absent "edit")
                      (ltu/is-operation-present "delete")
                      (ltu/is-operation-present (:validate c/action-uri))
                      :response
                      :body)]
        (is (= "admin@example.com" (:address email)))
        (is (false? (:validated? email))))

      ;; verify contents of user email
      (let [email (-> session-user
                      (request user-abs-uri)
                      (ltu/body->edn)
                      (ltu/is-status 200)
                      (ltu/is-operation-absent "edit")
                      (ltu/is-operation-present "delete")
                      (ltu/is-operation-present (:validate c/action-uri))
                      :response
                      :body)]
        (is (= "user@example.com" (:address email)))
        (is (false? (:validated? email))))

      ;; admin can delete the email
      (-> session-admin
          (request admin-abs-uri :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; user can delete the email
      (-> session-user
          (request user-abs-uri :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id t/resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [resource-uri :put]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (-> (session (ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
