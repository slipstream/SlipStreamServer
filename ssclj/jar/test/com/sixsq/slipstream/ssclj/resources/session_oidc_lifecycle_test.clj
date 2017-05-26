(ns com.sixsq.slipstream.ssclj.resources.session-oidc-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [ring.util.codec :as codec]

    [com.sixsq.slipstream.auth.cyclone :as auth-oidc]
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.session :as session]
    [com.sixsq.slipstream.ssclj.resources.session-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.session-template-oidc :as oidc]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase session/resource-name)))

(def session-template-base-uri (str p/service-context (u/de-camelcase ct/resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in SessionTemplate test examples
(dyn/initialize)

(def session-template-internal {:method      oidc/authn-method
                                :methodKey   oidc/authn-method
                                :name        "OpenID Connect"
                                :description "External Authentication via OpenID Connect Protocol"
                                :acl         st/resource-acl})

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
                         (header authn-info-header "unknown ANON"))
        session-anon-form (-> (session (ring-app))
                              (content-type session/form-urlencoded)
                              (header "content-type" session/form-urlencoded)
                              (header authn-info-header "unknown ANON"))
        redirect-uri "https://example.com/webui"]

    ;; get session template so that session resources can be tested
    (let [
          ;;
          ;; create the session template to use for these tests
          ;;
          href (-> session-admin
                   (request session-template-base-uri
                            :request-method :post
                            :body (json/write-str session-template-internal))
                   (ltu/body->edn)
                   (ltu/is-status 201)
                   (ltu/location))

          template-url (str p/service-context href)

          ;;href (str ct/resource-url "/" oidc/authn-method)
          ;;template-url (str p/service-context ct/resource-url "/" oidc/authn-method)
          resp (-> session-anon
                   (request template-url)
                   (ltu/body->edn)
                   (ltu/is-status 200))
          template (get-in resp [:response :body])
          valid-create {:sessionTemplate (strip-unwanted-attrs template)}
          href-create {:sessionTemplate {:href        href
                                         :redirectURI redirect-uri}}
          invalid-create (assoc-in valid-create [:sessionTemplate :invalid] "BAD")]

      ;; anonymous query should succeed but have no entries
      (-> session-anon
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-count zero?))

      ;; configuration must have OIDC client id and base URL, if not should get 500
      (-> session-anon
          (request base-uri
                   :request-method :post
                   :body (json/write-str valid-create))
          (ltu/body->edn)
          (ltu/message-matches #".*missing client ID, base URL, or public key.*")
          (ltu/is-status 500))

      ;; anonymous create must succeed (normal create and href create)
      (let [public-key (:auth-public-key environ.core/env)
            good-claims {:name  "OIDC_USER"
                         :email "user@oidc.example.com"}
            good-token (sign/sign-claims good-claims)
            bad-claims {}
            bad-token (sign/sign-claims bad-claims)]
        (with-redefs [environ.core/env (merge environ.core/env
                                              {:oidc-client-id  "FAKE_CLIENT_ID"
                                               :oidc-base-url   "https://oidc.example.com"
                                               :oidc-public-key public-key})]

          (let [resp (-> session-anon
                         (request base-uri
                                  :request-method :post
                                  :body (json/write-str valid-create))
                         (ltu/body->edn)
                         (ltu/is-status 303))
                id (get-in resp [:response :body :resource-id])
                uri (-> resp
                        (ltu/location))
                abs-uri (str p/service-context id)

                resp (-> session-anon
                         (request base-uri
                                  :request-method :post
                                  :body (json/write-str href-create))
                         (ltu/body->edn)
                         (ltu/is-status 303))
                id2 (get-in resp [:response :body :resource-id])
                uri2 (-> resp
                         (ltu/location))
                abs-uri2 (str p/service-context id2)

                resp (-> session-anon-form
                         (request base-uri
                                  :request-method :post
                                  :body (codec/form-encode {:href        href
                                                            :redirectURI redirect-uri}))
                         (ltu/body->edn)
                         (ltu/is-status 303))
                id3 (get-in resp [:response :body :resource-id])
                uri3 (-> resp
                         (ltu/location))
                abs-uri3 (str p/service-context id3)]

            ;; redirect URLs in location header should contain the client ID and resource id
            (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri "")))
            (is (re-matches (re-pattern (str ".*" (codec/url-encode id) ".*")) (or uri "")))
            (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri2 "")))
            (is (re-matches (re-pattern (str ".*" (codec/url-encode id2) ".*")) (or uri2 "")))
            (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri3 "")))
            (is (re-matches (re-pattern (str ".*" (codec/url-encode id3) ".*")) (or uri3 "")))

            ;; user should not be able to see session without session role
            (-> session-user
                (request abs-uri)
                (ltu/body->edn)
                (ltu/is-status 403))
            (-> session-user
                (request abs-uri2)
                (ltu/body->edn)
                (ltu/is-status 403))
            (-> session-user
                (request abs-uri3)
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
            (-> session-user
                (header authn-info-header (str "user USER ANON " id3))
                (request abs-uri3)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-id id3)
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
            (-> session-user
                (header authn-info-header (str "user USER ANON " id3))
                (request base-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-count 1))

            ;;
            ;; test validation callback
            ;;
            (let [validate-url (str abs-uri "/validate")
                  validate-url2 (str abs-uri2 "/validate")
                  validate-url3 (str abs-uri3 "/validate")]

              ;; try hitting the callback with an invalid server configuration
              (with-redefs [environ.core/env {}]

                (-> session-anon
                    (request validate-url
                             :request-method :get)
                    (ltu/body->edn)
                    (ltu/message-matches #".*missing client ID, base URL, or public key.*")
                    (ltu/is-status 500))

                (-> session-anon
                    (request validate-url2
                             :request-method :get)
                    (ltu/body->edn)
                    (ltu/message-matches #".*missing client ID, base URL, or public key.*")
                    (ltu/is-status 303))                    ;; always expect redirect when redirectURI is provided

                (-> session-anon
                    (request validate-url3
                             :request-method :get)
                    (ltu/body->edn)
                    (ltu/message-matches #".*missing client ID, base URL, or public key.*")
                    (ltu/is-status 303)))                   ;; alwasys expect redirect when redirectURI is provided

              ;; try hitting the callback without the OIDC code parameter
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
                  (ltu/is-status 303))                      ;; always expect redirect when redirectURI is provided

              (-> session-anon
                  (request validate-url3
                           :request-method :get)
                  (ltu/body->edn)
                  (ltu/message-matches #".*not contain required code.*")
                  (ltu/is-status 303))                      ;; always expect redirect when redirectURI is provided

              ;; try now with a fake code
              (with-redefs [auth-oidc/get-oidc-access-token (fn [client-id client-secret oauth-code redirect-url]
                                                              (case oauth-code
                                                                "GOOD" good-token
                                                                "BAD" bad-token
                                                                nil))
                            ex/match-external-user! (fn [authn-method external-login external-email]
                                                      ["MATCHED_USER" "/dashboard"])
                            db/find-roles-for-username (fn [username]
                                                         "USER ANON alpha")]

                (-> session-anon
                    (request (str validate-url "?code=NONE")
                             :request-method :get)
                    (ltu/body->edn)
                    (ltu/message-matches #".*unable to retrieve OIDC access token.*")
                    (ltu/is-status 400))

                (-> session-anon
                    (request (str validate-url "?code=BAD")
                             :request-method :get)
                    (ltu/body->edn)
                    (ltu/message-matches #".*OIDC token is missing name/preferred_name.*")
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
                  (is (re-matches (re-pattern (str ".*" id ".*")) (or (:roles claims) ""))))

                (let [ring-info (-> session-anon
                                    (request (str validate-url2 "?code=GOOD")
                                             :request-method :get)
                                    (ltu/body->edn)
                                    (ltu/is-status 303)
                                    (ltu/is-set-cookie))
                      location (ltu/location ring-info)
                      token (get-in ring-info [:response :cookies "com.sixsq.slipstream.cookie" :value :token])
                      claims (if token (sign/unsign-claims token) {})]
                  (is (= location redirect-uri))
                  (is (= "MATCHED_USER" (:username claims)))
                  (is (re-matches (re-pattern (str ".*" id2 ".*")) (or (:roles claims) ""))))

                (let [ring-info (-> session-anon
                                    (request (str validate-url3 "?code=GOOD")
                                             :request-method :get)
                                    (ltu/body->edn)
                                    (ltu/is-status 303)
                                    (ltu/is-set-cookie))
                      location (ltu/location ring-info)
                      token (get-in ring-info [:response :cookies "com.sixsq.slipstream.cookie" :value :token])
                      claims (if token (sign/unsign-claims token) {})]
                  (is (= location redirect-uri))
                  (is (= "MATCHED_USER" (:username claims)))
                  (is (re-matches (re-pattern (str ".*" id3 ".*")) (or (:roles claims) ""))))))

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

            (let [ring-info (-> session-user
                                (header authn-info-header (str "user USER ANON " id2))
                                (request abs-uri2)
                                (ltu/body->edn)
                                (ltu/is-status 200)
                                (ltu/is-id id2)
                                (ltu/is-operation-present "delete")
                                (ltu/is-operation-absent (:validate c/action-uri))
                                (ltu/is-operation-absent "edit"))
                  session (get-in ring-info [:response :body])]
              (is (= "MATCHED_USER" (:username session)))
              (is (not= (:created session) (:updated session))))

            (let [ring-info (-> session-user
                                (header authn-info-header (str "user USER ANON " id3))
                                (request abs-uri3)
                                (ltu/body->edn)
                                (ltu/is-status 200)
                                (ltu/is-id id3)
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
            (-> session-user
                (header authn-info-header (str "user USER ANON " id3))
                (request abs-uri3
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
                (ltu/is-status 400))))))))

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
