(ns com.sixsq.slipstream.ssclj.resources.callback-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase callback/resource-url)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(deftest lifecycle
  (let [session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "root ADMIN USER ANON"))
        session-jane (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))]

    ;; admin user collection query should succeed but be empty (no  records created yet)
    (-> session-admin
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; normal user collection query should not succeed
    (-> session-jane
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))


    ;; anonymous credential collection query should not succeed
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))


    ;; create a callback as a admin user
    (let [
          create-test-callback {:action "action-name"}

          resp-test (-> session-admin
                        (request base-uri
                                 :request-method :post
                                 :body (json/write-str create-test-callback))
                        (ltu/body->edn)
                        (ltu/is-status 201))

          id-test (get-in resp-test [:response :body :resource-id])

          location-test (str p/service-context (-> resp-test ltu/location))

          test-uri (str p/service-context id-test)]

      (is (= location-test test-uri))

      ;; admin should be able to see records
      (-> session-admin
          (request test-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-operation-present "delete")
          (ltu/is-operation-absent "edit")
          (ltu/dump)
          (ltu/is-operation-present (:validate c/action-uri)))

      (-> session-jane
          (request test-uri)
          (ltu/body->edn)
          (ltu/is-status 403))

      ;; check contents and editing
      (let [reread-test-callback (-> session-admin
                                     (request test-uri)
                                     (ltu/body->edn)
                                     (ltu/is-status 200)
                                     :response
                                     :body)]

        (is (= (strip-unwanted-attrs reread-test-callback)
               (strip-unwanted-attrs (assoc create-test-callback :state "WAITING")))))

      ;; disallowed edits
      (-> session-jane
          (request test-uri
                   :request-method :put
                   :body (json/write-str create-test-callback))
          (ltu/body->edn)
          (ltu/is-status 405))

      (-> session-anon
          (request test-uri
                   :request-method :put
                   :body (json/write-str create-test-callback))
          (ltu/body->edn)
          (ltu/is-status 405))

      ;; search
      (-> session-admin
          (request base-uri
                   :request-method :put
                   :body (json/write-str create-test-callback))
          (ltu/body->edn)
          (ltu/is-count 1)
          (ltu/is-status 200))

      ;;delete
      (-> session-jane
          (request test-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 403))

      (-> session-admin
          (request test-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;;record should be deleted
      (-> session-admin
          (request test-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 404)))))


(deftest bad-methods

  (let [resource-uri (str p/service-context (u/new-resource-id callback/resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (-> (session (ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
