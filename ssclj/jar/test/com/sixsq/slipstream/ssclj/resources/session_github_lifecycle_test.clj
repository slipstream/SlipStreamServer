(ns com.sixsq.slipstream.ssclj.resources.session-github-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.session :as session]
    [com.sixsq.slipstream.ssclj.resources.session-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.session-template-github :as github]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase session/resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in SessionTemplate test examples
(dyn/initialize)

(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(deftest lifecycle

  (let [session-anon (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))]

    ;; get session template so that session resources can be tested
    (let [href (str ct/resource-url "/" github/authn-method)
          template-url (str p/service-context ct/resource-url "/" github/authn-method)
          resp (-> session-anon
                   (request template-url)
                   (ltu/body->edn)
                   (ltu/is-status 200))
          template (get-in resp [:response :body])
          valid-create {:sessionTemplate (strip-unwanted-attrs template)}
          href-create {:sessionTemplate {:href href}}
          invalid-create (assoc-in valid-create [:sessionTemplate :invalid] "BAD")]

      ;; anonymous query should succeed but have no entries
      (-> session-anon
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-count zero?))

      ;; configuration must have GitHub client ID, if not should get 500
      (-> session-anon
          (request base-uri
                   :request-method :post
                   :body (json/write-str valid-create))
          (ltu/body->edn)
          (ltu/message-matches #".*missing client ID.*")
          (ltu/is-status 500))

      ;; anonymous create must succeed (normal create and href create)
      (with-redefs [environ.core/env {:github-client-id "FAKE_CLIENT_ID"}]

        (let [resp (-> session-anon
                       (request base-uri
                                :request-method :post
                                :body (json/write-str valid-create))
                       (ltu/body->edn)
                       (ltu/is-status 307))
              id (get-in resp [:response :body :resource-id])
              uri (-> resp
                      (ltu/location))
              abs-uri (str p/service-context (u/de-camelcase uri))

              resp (-> session-anon
                       (request base-uri
                                :request-method :post
                                :body (json/write-str href-create))
                       (ltu/body->edn)
                       (ltu/is-status 307))
              id2 (get-in resp [:response :body :resource-id])
              uri2 (-> resp
                       (ltu/location))
              abs-uri2 (str p/service-context (u/de-camelcase uri2))]

          ;; redirect URLs in location header should contain the client ID
          (is (re-matches #".*FAKE_CLIENT_ID.*" uri))
          (is (re-matches #".*FAKE_CLIENT_ID.*" uri2))


          ;; user should not be able to see session without session role
          #_(-> (session (ring-app))
                (header authn-info-header "user USER")
                (request abs-uri)
                (ltu/body->edn)
                (ltu/is-status 403))
          #_(-> (session (ring-app))
                (header authn-info-header "user USER")
                (request abs-uri2)
                (ltu/body->edn)
                (ltu/is-status 403))

          ;; anonymous query should succeed but still have no entries
          #_(-> (session (ring-app))
                (request base-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-count zero?))

          ;; user query should succeed but have no entries because of missing session role
          #_(-> (session (ring-app))
                (header authn-info-header "user USER")
                (request base-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-count zero?))

          ;; admin query should succeed, but see no sessions without the correct session role
          #_(-> (session (ring-app))
                (header authn-info-header "root ADMIN")
                (request base-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-count 0))

          ;; user should be able to see session with session role
          #_(-> (session (ring-app))
                (header authn-info-header (str "user USER " id))
                (request abs-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-id id)
                (ltu/is-operation-present "delete")
                (ltu/is-operation-absent "edit"))
          #_(-> (session (ring-app))
                (header authn-info-header (str "user USER " id2))
                (request abs-uri2)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-id id2)
                (ltu/is-operation-present "delete")
                (ltu/is-operation-absent "edit"))

          ;; user query with session role should succeed but and have one entry
          #_(-> (session (ring-app))
                (header authn-info-header (str "user USER " id))
                (request base-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-count 1))
          #_(-> (session (ring-app))
                (header authn-info-header (str "user USER " id2))
                (request base-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-count 1))

          ;; user with session role can delete resource
          #_(-> (session (ring-app))
                (header authn-info-header (str "user USER " id))
                (request abs-uri
                         :request-method :delete)
                (ltu/is-unset-cookie)
                (ltu/body->edn)
                (ltu/is-status 200))
          #_(-> (session (ring-app))
                (header authn-info-header (str "user USER " id2))
                (request abs-uri2
                         :request-method :delete)
                (ltu/is-unset-cookie)
                (ltu/body->edn)
                (ltu/is-status 200))

          ;; create with invalid template fails
          #_(-> (session (ring-app))
                (content-type "application/json")
                (request base-uri
                         :request-method :post
                         :body (json/write-str invalid-create))
                (ltu/body->edn)
                (ltu/is-status 400))))

      ;; admin create must also succeed
      #_(let [create-req (-> valid-create
                             (assoc-in [:sessionTemplate :username] "root")
                             (assoc-in [:sessionTemplate :password] "root"))
              resp (-> (session (ring-app))
                       (content-type "application/json")
                       (request base-uri
                                :request-method :post
                                :body (json/write-str create-req))
                       (ltu/is-set-cookie)
                       (ltu/body->edn)
                       (ltu/is-status 201))
              id (get-in resp [:response :body :resource-id])
              uri (-> resp
                      (ltu/location))
              abs-uri (str p/service-context (u/de-camelcase uri))]

          ;; admin should be able to see and delete session with session role
          (-> (session (ring-app))
              (header authn-info-header (str "root ADMIN " id))
              (request abs-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-operation-present "delete")
              (ltu/is-operation-absent "edit"))

          ;; admin can delete resource with session role
          (-> (session (ring-app))
              (header authn-info-header (str "root ADMIN " id))
              (request abs-uri
                       :request-method :delete)
              (ltu/is-unset-cookie)
              (ltu/body->edn)
              (ltu/is-status 200)))

      ;; admin create with invalid template fails
      #_(-> (session (ring-app))
            (content-type "application/json")
            (header authn-info-header "root ADMIN")
            (request base-uri
                     :request-method :post
                     :body (json/write-str invalid-create))
            (ltu/body->edn)
            (ltu/is-status 400)))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id session/resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [resource-uri :options]
                          [resource-uri :put]
                          [resource-uri :post]]]
        (-> (session (ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
