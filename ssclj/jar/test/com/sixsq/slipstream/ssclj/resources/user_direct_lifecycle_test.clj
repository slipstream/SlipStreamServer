(ns com.sixsq.slipstream.ssclj.resources.user-direct-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.user :as user]
    [com.sixsq.slipstream.ssclj.resources.user-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.user-template-direct :as direct]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase user/resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in SessionTemplate test examples
(dyn/initialize)

(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(deftest lifecycle

  (let [href (str ct/resource-url "/" direct/registration-method)
        template-url (str p/service-context ct/resource-url "/" direct/registration-method)
        resp (-> (session (ring-app))
                 (content-type "application/json")
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))
        template (get-in resp [:response :body])
        valid-create {:userTemplate (strip-unwanted-attrs (assoc template
                                                            :username "user"
                                                            :emailAddress "user@example.org"))}
        href-create {:userTemplate {:href         href
                                    :username     "user"
                                    :emailAddress "user@example.org"}}
        invalid-create (assoc-in valid-create [:userTemplate :invalid] "BAD")]

    ;; create a new user as administrator
    #_(-> (session (ring-app))
          (content-type "application/json")
          (header authn-info-header "root ADMIN")
          (request base-uri
                   :request-method :post
                   :body (json/write-str valid-create))
          (ltu/body->edn)
          (ltu/is-status 201))

    ;; anonymous query should succeed but have no entries
    #_(-> (session (ring-app))
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-count zero?))

    ;; anonymous create must NOT succeed (normal create)
    #_(-> (session (ring-app))
          (content-type "application/json")
          (header authn-info-header "unknown ANON")
          (request base-uri
                   :request-method :post
                   :body (json/write-str valid-create))
          (ltu/body->edn)
          (ltu/is-status 403))

    ;; anonymous create must NOT succeed (href create)
    #_(-> (session (ring-app))
          (content-type "application/json")
          (header authn-info-header "unknown ANON")
          (request base-uri
                   :request-method :post
                   :body (json/write-str href-create))
          (ltu/body->edn)
          (ltu/is-status 403))

    ;; admin create must also succeed
    #_(let [create-req (-> valid-create
                           (assoc-in [:userTemplate :username] "root")
                           (assoc-in [:userTemplate :emailAddress] "root@example.com"))
            resp (-> (session (ring-app))
                     (content-type "application/json")
                     (header authn-info-header "root ADMIN")
                     (request base-uri
                              :request-method :post
                              :body (json/write-str create-req))
                     (ltu/body->edn)
                     (ltu/is-status 201))
            id (get-in resp [:response :body :resource-id])
            uri (-> resp
                    (ltu/location))
            abs-uri (str p/service-context (u/de-camelcase uri))]

        ;; admin should be able to see and delete user with user role
        (-> (session (ring-app))
            (header authn-info-header (str "root ADMIN " id))
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-operation-present "delete")
            (ltu/is-operation-absent "edit"))

        ;; admin can delete resource with user role
        (-> (session (ring-app))
            (header authn-info-header (str "root ADMIN " id))
            (request abs-uri
                     :request-method :delete)
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
          (ltu/is-status 400))


    (let [href (str ct/resource-url "/" direct/registration-method)
          template-url (str p/service-context ct/resource-url "/" direct/registration-method)
          resp (-> (session (ring-app))
                   (content-type "application/json")
                   (request template-url)
                   (ltu/body->edn)
                   (ltu/is-status 200))
          template (get-in resp [:response :body])
          valid-create {:userTemplate (strip-unwanted-attrs (assoc template
                                                              :username "user"
                                                              :emailAddress "user@example.org"))}
          href-create {:userTemplate {:href         href
                                      :username     "user"
                                      :emailAddress "user@example.org"}}
          invalid-create (assoc-in valid-create [:userTemplate :invalid] "BAD")]

      ;; create a new user as administrator
      #_(-> (session (ring-app))
            (content-type "application/json")
            (header authn-info-header "root ADMIN")
            (request base-uri
                     :request-method :post
                     :body (json/write-str valid-create))
            (ltu/body->edn)
            (ltu/is-status 201))

      ;; anonymous query should succeed but have no entries
      #_(-> (session (ring-app))
            (request base-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-count zero?))

      ;; anonymous create must NOT succeed (normal create)
      #_(-> (session (ring-app))
            (content-type "application/json")
            (header authn-info-header "unknown ANON")
            (request base-uri
                     :request-method :post
                     :body (json/write-str valid-create))
            (ltu/body->edn)
            (ltu/is-status 403))

      ;; anonymous create must NOT succeed (href create)
      #_(-> (session (ring-app))
            (content-type "application/json")
            (header authn-info-header "unknown ANON")
            (request base-uri
                     :request-method :post
                     :body (json/write-str href-create))
            (ltu/body->edn)
            (ltu/is-status 403))

      ;; admin create must also succeed
      #_(let [create-req (-> valid-create
                             (assoc-in [:userTemplate :username] "root")
                             (assoc-in [:userTemplate :emailAddress] "root@example.com"))
              resp (-> (session (ring-app))
                       (content-type "application/json")
                       (header authn-info-header "root ADMIN")
                       (request base-uri
                                :request-method :post
                                :body (json/write-str create-req))
                       (ltu/body->edn)
                       (ltu/is-status 201))
              id (get-in resp [:response :body :resource-id])
              uri (-> resp
                      (ltu/location))
              abs-uri (str p/service-context (u/de-camelcase uri))]

          ;; admin should be able to see and delete user with user role
          (-> (session (ring-app))
              (header authn-info-header (str "root ADMIN " id))
              (request abs-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-operation-present "delete")
              (ltu/is-operation-absent "edit"))

          ;; admin can delete resource with user role
          (-> (session (ring-app))
              (header authn-info-header (str "root ADMIN " id))
              (request abs-uri
                       :request-method :delete)
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
  (let [resource-uri (str p/service-context (u/new-resource-id user/resource-name))]
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
