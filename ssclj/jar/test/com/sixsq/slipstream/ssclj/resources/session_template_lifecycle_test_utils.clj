(ns com.sixsq.slipstream.ssclj.resources.session-template-lifecycle-test-utils
  (:require
    [clojure.test :refer :all]
    [clojure.set :as set]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.session-template :refer :all]
    [com.sixsq.slipstream.ssclj.resources.session-template-internal :as internal]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(defn session-template-lifecycle [base-uri ring-app valid-template]

  (let [session-admin (-> ring-app
                          session
                          (content-type "application/json")
                          (header authn-info-header "root ADMIN USER ANON"))
        session-user (-> ring-app
                         session
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> ring-app
                         session
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))]

    ;; all view actions should be available to anonymous users
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-resource-uri collection-uri)
        (ltu/is-count zero?)
        (ltu/is-operation-absent "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit")
        (ltu/is-operation-absent "describe"))

    ;; for admin, should be able to add as well
    (-> session-admin
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-resource-uri collection-uri)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit")
        (ltu/is-operation-absent "describe"))

    ;; creating with an unknown authentication method should fail
    (-> session-admin
        (request base-uri
                 :request-method :post
                 :body (json/write-str (assoc valid-template :method "UNKNOWN")))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; do full lifecycle for an internal session template
    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-template))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; ensure that the created template can be retrieved by anyone
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-operation-absent "add")
          (ltu/is-operation-present "delete")
          (ltu/is-operation-present "edit")
          (ltu/is-operation-present "describe"))

      (-> session-user
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-operation-absent "add")
          (ltu/is-operation-absent "delete")
          (ltu/is-operation-absent "edit")
          (ltu/is-operation-present "describe"))

      ;; anon can read the template and additionally check that the describe works
      (let [describe-href (-> session-anon
                              (request abs-uri)
                              (ltu/body->edn)
                              (ltu/is-status 200)
                              (ltu/is-operation-absent "add")
                              (ltu/is-operation-absent "delete")
                              (ltu/is-operation-absent "edit")
                              (ltu/is-operation-present "describe")
                              (ltu/get-op "describe"))
            describe-url (str p/service-context describe-href)]

        (-> session-anon
            (request describe-url)
            (ltu/body->edn)
            (ltu/is-status 200)))

      ;; verify that the id corresponds to the value in the instance parameter
      (let [{:keys [id instance]} (-> session-anon
                                      (request abs-uri)
                                      (ltu/body->edn)
                                      :response
                                      :body)]
        (is (= id (str resource-url "/" instance))))

      ;; verify that editing/updating the template works
      (let [orig-template (-> session-anon
                              (request abs-uri)
                              (ltu/body->edn)
                              :response
                              :body)
            updated-template (assoc orig-template :name "UPDATED_NAME")]

        (-> session-admin
            (request abs-uri
                     :request-method :put
                     :body (json/write-str updated-template))
            (ltu/body->edn)
            (ltu/is-status 200))

        (let [reread-template (-> session-anon
                                  (request abs-uri)
                                  (ltu/body->edn)
                                  :response
                                  :body)]

          (is (= (dissoc orig-template :name :updated) (dissoc reread-template :name :updated)))
          (is (= "UPDATED_NAME" (:name reread-template)))
          (is (not= (:name orig-template) (:name reread-template)))
          (is (not= (:updated orig-template) (:updated reread-template)))))

      ;; session template should be visible via query as well
      (-> session-anon
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-resource-uri collection-uri)
          (ltu/is-count #(= 1 %)))

      ;; delete the template
      (-> session-admin
          (request abs-uri :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; verify that the template is gone
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404))

      ;; session template should not be there anymore
      ;; unreliable test because the update of the search index in
      ;; elasticsearch is asynchronous.
      #_(-> session-anon
            (request base-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-resource-uri collection-uri)
            (ltu/is-count zero?)))))

(defn bad-methods [base-uri ring-app]
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))
        session (session ring-app)]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (-> session
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
