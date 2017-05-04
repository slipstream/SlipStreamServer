(ns com.sixsq.slipstream.ssclj.resources.session-github-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [ring.util.codec :as codec]

    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.session :as session]
    [com.sixsq.slipstream.ssclj.resources.session-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.session-template-github :as github]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.auth.github :as auth-github]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.auth.external :as ex]))

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

  (let [session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "admin ADMIN USER ANON"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "user USER ANON"))
        session-anon (-> (session (ring-app))
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

      ;; configuration must have GitHub client ID or secret, if not should get 500
      (-> session-anon
          (request base-uri
                   :request-method :post
                   :body (json/write-str valid-create))
          (ltu/body->edn)
          (ltu/message-matches #".*missing client ID.*")
          (ltu/is-status 500))

      ;; anonymous create must succeed (normal create and href create)
      (with-redefs [environ.core/env (merge environ.core/env
                                            {:github-client-id     "FAKE_CLIENT_ID"
                                             :github-client-secret "FAKE_CLIENT_SECRET"})]

        (let [resp (-> session-anon
                       (request base-uri
                                :request-method :post
                                :body (json/write-str valid-create))
                       (ltu/body->edn)
                       (ltu/is-status 307))
              id (get-in resp [:response :body :resource-id])
              uri (-> resp
                      (ltu/location))
              abs-uri (str p/service-context id)

              resp (-> session-anon
                       (request base-uri
                                :request-method :post
                                :body (json/write-str href-create))
                       (ltu/body->edn)
                       (ltu/is-status 307))
              id2 (get-in resp [:response :body :resource-id])
              uri2 (-> resp
                       (ltu/location))
              abs-uri2 (str p/service-context id2)]

          ;; redirect URLs in location header should contain the client ID and resource id
          (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri "")))
          (is (re-matches (re-pattern (str ".*" (codec/url-encode id) ".*")) (or uri "")))
          (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri2 "")))
          (is (re-matches (re-pattern (str ".*" (codec/url-encode id2) ".*")) (or uri2 "")))

          ;; user should not be able to see session without session role
          (-> session-user
              (request abs-uri)
              (ltu/body->edn)
              (ltu/is-status 403))
          (-> session-user
              (request abs-uri2)
              (ltu/body->edn)
              (ltu/is-status 403))

          ;; anonymous query should succeed but still have no entries
          (-> session-anon
              (request base-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-count zero?))

          ;; user query should succeed but have no entries because of missing session role
          (-> session-user
              (request base-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-count zero?))

          ;; admin query should succeed, but see no sessions without the correct session role
          (-> session-admin
              (request base-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-count 0))

          ;; user should be able to see session with session role
          (-> session-user
              (header authn-info-header (str "user USER ANON " id))
              (request abs-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-id id)
              (ltu/is-operation-present "delete")
              (ltu/is-operation-present (:validate c/action-uri))
              (ltu/is-operation-absent "edit"))
          (-> session-user
              (header authn-info-header (str "user USER ANON " id2))
              (request abs-uri2)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-id id2)
              (ltu/is-operation-present "delete")
              (ltu/is-operation-present (:validate c/action-uri))
              (ltu/is-operation-absent "edit"))

          ;; user query with session role should succeed but and have one entry
          (-> session-user
              (header authn-info-header (str "user USER ANON " id))
              (request base-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-count 1))
          (-> session-user
              (header authn-info-header (str "user USER ANON " id2))
              (request base-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-count 1))

          ;;
          ;; test validation callback
          ;;
          (let [validate-url (str abs-uri "/validate")
                validate-url2 (str abs-uri2 "/validate")]

            ;; try hitting the callback with an invalid server configuration
            (with-redefs [environ.core/env {}]

              (-> session-anon
                  (request validate-url
                           :request-method :get)
                  (ltu/body->edn)
                  (ltu/message-matches #".*missing client ID.*")
                  (ltu/is-status 500))

              (-> session-anon
                  (request validate-url2
                           :request-method :get)
                  (ltu/body->edn)
                  (ltu/message-matches #".*missing client ID.*")
                  (ltu/is-status 500)))

            ;; try hitting the callback without the OAuth code parameter
            (-> session-anon
                (request validate-url
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*not contain required code.*")
                (ltu/is-status 400))

            (-> session-anon
                (request validate-url2
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*not contain required code.*")
                (ltu/is-status 400))

            ;; try now with a fake code
            (with-redefs [auth-github/get-github-access-token (fn [client-id client-secret oauth-code]
                                                                (case oauth-code
                                                                  "GOOD" "GOOD_ACCESS_CODE"
                                                                  "BAD" "BAD_ACCESS_CODE"
                                                                  nil))
                          auth-github/get-github-user-info (fn [access-code]
                                                             (when (= access-code "GOOD_ACCESS_CODE")
                                                               {:login "GITHUB_USER"
                                                                :email "user@example.com"}))

                          ex/match-external-user! (fn [authn-method external-login external-email]
                                                    ["MATCHED_USER" "/dashboard"])

                          db/find-roles-for-username (fn [username]
                                                       "USER ANON alpha")]

              (-> session-anon
                  (request (str validate-url "?code=NONE")
                           :request-method :get)
                  (ltu/body->edn)
                  (ltu/message-matches #".*unable to retrieve GitHub access code.*")
                  (ltu/is-status 400))

              (-> session-anon
                  (request (str validate-url "?code=BAD")
                           :request-method :get)
                  (ltu/body->edn)
                  (ltu/message-matches #".*unable to retrieve GitHub user information.*")
                  (ltu/is-status 400))

              (let [ring-info (-> session-anon
                                  (request (str validate-url "?code=GOOD")
                                           :request-method :get)
                                  (ltu/body->edn)
                                  (ltu/is-status 201)
                                  (ltu/is-set-cookie))
                    location (ltu/location ring-info)
                    token (get-in ring-info [:response :cookies "com.sixsq.slipstream.cookie" :value :token])
                    claims (if token (sign/unsign-claims token) {})]
                (is (= location id))
                (is (= "MATCHED_USER" (:username claims)))
                (is (re-matches (re-pattern (str ".*" id ".*")) (or (:roles claims) ""))))))

          ;; check that the session has been updated
          (let [ring-info (-> session-user
                              (header authn-info-header (str "user USER ANON " id))
                              (request abs-uri)
                              (ltu/body->edn)
                              (ltu/is-status 200)
                              (ltu/is-id id)
                              (ltu/is-operation-present "delete")
                              (ltu/is-operation-absent (:validate c/action-uri))
                              (ltu/is-operation-absent "edit"))
                session (get-in ring-info [:response :body])]
            (is (= "MATCHED_USER" (:username session)))
            (is (not= (:created session) (:updated session))))

          ;; user with session role can delete resource
          (-> session-user
              (header authn-info-header (str "user USER ANON " id))
              (request abs-uri
                       :request-method :delete)
              (ltu/is-unset-cookie)
              (ltu/body->edn)
              (ltu/is-status 200))
          (-> session-user
              (header authn-info-header (str "user USER ANON " id2))
              (request abs-uri2
                       :request-method :delete)
              (ltu/is-unset-cookie)
              (ltu/body->edn)
              (ltu/is-status 200))

          ;; create with invalid template fails
          (-> session-anon
              (request base-uri
                       :request-method :post
                       :body (json/write-str invalid-create))
              (ltu/body->edn)
              (ltu/is-status 400)))))))

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
