(ns com.sixsq.slipstream.ssclj.resources.session-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.session :refer :all]
    [com.sixsq.slipstream.ssclj.resources.session-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.session-template-internal :as example]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in SessionTemplate test examples
(dyn/initialize)

(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(defn mock-login-valid?
  "Will return true if the username and password are identical;
   false otherwise.  Avoids having to start a real database and
   populate it with users."
  [{:keys [username password]}]
  (= username password))

(defn mock-roles
  "Mocking function to return the roles for a given user.  For
   'root' the 'ADMIN' role will be added; for all others, the
   'USER' role will be added."
  [username]
  (case username
    "root" ["ADMIN"]
    ["USER"]))

(deftest lifecycle

  (with-redefs [auth-internal/valid? mock-login-valid?
                db/find-roles-for-username mock-roles]

    ;; check that the mocking is working correctly
    (is (auth-internal/valid? {:username "user" :password "user"}))
    (is (not (auth-internal/valid? {:username "user" :password "BAD"})))
    (is (= ["ADMIN"] (db/find-roles-for-username "root")))
    (is (= ["USER"] (db/find-roles-for-username "user")))

    (let [href (str ct/resource-url "/" example/authn-method)
          template-url (str p/service-context ct/resource-url "/" example/authn-method)
          resp (-> (session (ring-app))
                   (content-type "application/json")
                   (header authn-info-header "root ADMIN")
                   (request template-url)
                   (ltu/body->json)
                   (ltu/is-status 200))
          template (get-in resp [:response :body])
          valid-create {:sessionTemplate (strip-unwanted-attrs (assoc template :username "user" :password "user"))}
          href-create {:sessionTemplate {:href     href
                                         :username "user"
                                         :password "user"}}
          invalid-create (assoc-in valid-create [:sessionTemplate :invalid] "BAD")]

      ;; anonymous create must succeed
      (let [uri (-> (session (ring-app))
                    (content-type "application/json")
                    (header authn-info-header "unknown ANON")
                    (request base-uri
                             :request-method :post
                             :body (json/write-str valid-create))
                    (ltu/body->json)
                    (ltu/is-status 201)
                    (ltu/is-set-cookie)
                    (ltu/location))
            abs-uri (str p/service-context (u/de-camelcase uri))]

        (-> (session (ring-app))
            (header authn-info-header "user USER")
            (request abs-uri
                     :request-method :delete)
            (ltu/body->json)
            (ltu/is-status 200)
            (ltu/is-unset-cookie)))

      ;; admin create must also succeed (although not terribly useful)
      (let [create-req (-> valid-create
                           (assoc-in [:sessionTemplate :username] "root")
                           (assoc-in [:sessionTemplate :password] "root"))
            uri (-> (session (ring-app))
                    (content-type "application/json")
                    (header authn-info-header "root ADMIN")
                    (request base-uri
                             :request-method :post
                             :body (json/write-str create-req))
                    (ltu/body->json)
                    (ltu/is-status 201)
                    (ltu/is-set-cookie)
                    (ltu/location))
            abs-uri (str p/service-context (u/de-camelcase uri))]

        (-> (session (ring-app))
            (header authn-info-header "root ADMIN")
            (request abs-uri
                     :request-method :delete)
            (ltu/body->json)
            (ltu/is-status 200)
            (ltu/is-unset-cookie)))

      ;; admin create with invalid template fails
      (-> (session (ring-app))
          (content-type "application/json")
          (header authn-info-header "root ADMIN")
          (request base-uri
                   :request-method :post
                   :body (json/write-str invalid-create))
          (ltu/body->json)
          (ltu/is-status 400))

      ;; full session lifecycle as user must work
      (let [uri (-> (session (ring-app))
                    (content-type "application/json")
                    (header authn-info-header "user USER")
                    (request base-uri
                             :request-method :post
                             :body (json/write-str valid-create))
                    (ltu/body->json)
                    (ltu/is-status 201)
                    (ltu/location))
            abs-uri (str p/service-context (u/de-camelcase uri))]

        ;; anonymous query succeeds but should have no entries
        (-> (session (ring-app))
            (request base-uri)
            (header authn-info-header "root ADMIN")
            (ltu/body->json)
            (ltu/is-status 200)
            (ltu/is-count zero?))

        ;; user query succeeds
        (let [entries (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "user USER")
                          (request base-uri)
                          (ltu/body->json)
                          (ltu/is-status 200)
                          (ltu/is-resource-uri collection-uri)
                          (ltu/is-count #(= 1 %))
                          (ltu/entries resource-tag))]
          (is ((set (map :id entries)) uri))

          ;; verify that all entries are accessible
          (let [pair-fn (juxt :id #(str p/service-context (:id %)))
                pairs (map pair-fn entries)]
            (doseq [[id entry-uri] pairs]
              (-> (session (ring-app))
                  (header authn-info-header "user USER")
                  (request entry-uri)
                  (ltu/body->json)
                  (ltu/is-status 200)
                  (ltu/is-id id)))))

        ;; user delete succeeds
        (-> (session (ring-app))
            (header authn-info-header "user USER")
            (request abs-uri
                     :request-method :delete)
            (ltu/body->json)
            (ltu/is-status 200))

        ;; ensure entry is really gone
        (-> (session (ring-app))
            (header authn-info-header "user USER")
            (request abs-uri)
            (ltu/body->json)
            (ltu/is-status 404)))

      ;; abbreviated lifecycle using href to template instead of copy
      (let [uri (-> (session (ring-app))
                    (content-type "application/json")
                    (header authn-info-header "user USER")
                    (request base-uri
                             :request-method :post
                             :body (json/write-str href-create))
                    (ltu/body->json)
                    (ltu/is-status 201)
                    (ltu/location))
            abs-uri (str p/service-context (u/de-camelcase uri))]

        ;; user delete succeeds
        (-> (session (ring-app))
            (header authn-info-header "user USER")
            (request abs-uri
                     :request-method :delete)
            (ltu/body->json)
            (ltu/is-status 200))

        ;; ensure entry is really gone
        (-> (session (ring-app))
            (header authn-info-header "user USER")
            (request abs-uri)
            (ltu/body->json)
            (ltu/is-status 404))))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [base-uri :put]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (-> (session (ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
