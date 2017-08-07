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
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.configuration :as configuration]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase session/resource-name)))

(def configuration-base-uri (str p/service-context (u/de-camelcase configuration/resource-name)))

(def session-template-base-uri (str p/service-context (u/de-camelcase ct/resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in SessionTemplate test examples
(dyn/initialize)

(def instance "test-github")
(def session-template-github {:method      github/authn-method
                              :instance    instance
                              :name        "GitHub"
                              :description "External Authentication with GitHub Credentials"
                              :acl         st/resource-acl})

(def configuration-session-github {:configurationTemplate {:service      "session-github"
                                                           :instance     instance
                                                           :clientID     "FAKE_CLIENT_ID"
                                                           :clientSecret "ABCDEF..."}})

(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(deftest lifecycle

  (let [app (ring-app)
        session-admin (-> (session app)
                          (content-type "application/json")
                          (header authn-info-header "admin ADMIN USER ANON"))
        session-user (-> (session app)
                         (content-type "application/json")
                         (header authn-info-header "user USER ANON"))
        session-anon (-> (session app)
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))
        session-anon-form (-> (session app)
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
                            :body (json/write-str session-template-github))
                   (ltu/body->edn)
                   (ltu/is-status 201)
                   (ltu/location))

          template-url (str p/service-context href)

          resp (-> session-anon
                   (request template-url)
                   (ltu/body->edn)
                   (ltu/is-status 200))
          template (get-in resp [:response :body])
          href-create {:sessionTemplate {:href href}}
          href-create-redirect {:sessionTemplate {:href        href
                                                  :redirectURI redirect-uri}}
          invalid-create (assoc-in href-create-redirect [:sessionTemplate :invalid] "BAD")]

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
                   :body (json/write-str href-create))
          (ltu/body->edn)
          (ltu/message-matches #".*missing or incorrect configuration.*")
          (ltu/is-status 500))

      ;; anonymous create must succeed (normal create and href create)
      (let [
            ;;
            ;; create the session-github configuration to use for these tests
            ;;
            cfg-href (-> session-admin
                         (request configuration-base-uri
                                  :request-method :post
                                  :body (json/write-str configuration-session-github))
                         (ltu/body->edn)
                         (ltu/is-status 201)
                         (ltu/location))

            _ (is (= cfg-href (str "configuration/session-github-" instance)))

            resp (-> session-anon
                     (request base-uri
                              :request-method :post
                              :body (json/write-str href-create))
                     (ltu/body->edn)
                     (ltu/is-status 303))
            id (get-in resp [:response :body :resource-id])
            uri (-> resp
                    (ltu/location))
            abs-uri (str p/service-context id)

            resp (-> session-anon
                     (request base-uri
                              :request-method :post
                              :body (json/write-str href-create-redirect))
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

          ;; remove the authentication configuration
          (-> session-admin
              (request (str p/service-context cfg-href)
                       :request-method :delete)
              (ltu/body->edn)
              (ltu/is-status 200))

          ;; try hitting the callback with an invalid server configuration

          (-> session-anon
              (request validate-url
                       :request-method :get)
              (ltu/body->edn)
              (ltu/message-matches #".*missing or incorrect configuration.*")
              (ltu/is-status 500))

          (-> session-anon
              (request validate-url2
                       :request-method :get)
              (ltu/body->edn)
              (ltu/message-matches #".*missing or incorrect configuration.*")
              (ltu/is-status 303))                          ;; always expect redirect when redirectURI is provided

          (-> session-anon
              (request validate-url3
                       :request-method :get)
              (ltu/body->edn)
              (ltu/message-matches #".*missing or incorrect configuration.*")
              (ltu/is-status 303))                          ;; always expect redirect when redirectURI is provided

          ;; add the configuration back again
          (-> session-admin
              (request configuration-base-uri
                       :request-method :post
                       :body (json/write-str configuration-session-github))
              (ltu/body->edn)
              (ltu/is-status 201))

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
              (ltu/is-status 303))                          ;; always expect redirect when redirectURI is provided

          (-> session-anon
              (request validate-url3
                       :request-method :get)
              (ltu/body->edn)
              (ltu/message-matches #".*not contain required code.*")
              (ltu/is-status 303))                          ;; always expect redirect when redirectURI is provided

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
            (ltu/is-status 400))))))
