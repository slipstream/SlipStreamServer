(ns com.sixsq.slipstream.ssclj.resources.session-oidc-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [com.sixsq.slipstream.auth.oidc :as auth-oidc]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as cbu]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as configuration]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.session :as session]
    [com.sixsq.slipstream.ssclj.resources.session-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.session-template-oidc :as oidc]
    [peridot.core :refer :all]
    [ring.util.codec :as codec]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase session/resource-name)))

(def configuration-base-uri (str p/service-context (u/de-camelcase configuration/resource-name)))

(def session-template-base-uri (str p/service-context (u/de-camelcase ct/resource-name)))

(def instance "test-oidc")

(def session-template-oidc {:method      oidc/authn-method
                            :instance    instance
                            :name        "OpenID Connect"
                            :description "External Authentication via OpenID Connect Protocol"
                            :acl         st/resource-acl})


(def ^:const callback-pattern #".*/api/callback/.*/execute")

;; callback state reset between tests
(defn reset-callback! [callback-id]
  (cbu/update-callback-state! "WAITING" callback-id))

(def auth-pubkey
  (str
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA835H7CQt2oOmlj6GoZp+"
    "dFLE6k43Ybi3ku/yuuzatlnet95xVibbyD+DWBz8owRx5F7dZKbFuJPD7KNZWnxD"
    "P4hSO6p7xg6xOjWrU2naMW8SaWs8cbU7rssRKbEmCc39888pgNi6/VgZiHXmVeUR"
    "eWbxlrppIhIrRiHwf8LHA0LzGn0UAS4K0dMPdRR02vWs5hRw8yOAr0hXU2LUb7AO"
    "uP73cumiWDqkmJBhKa1PYN7vixkud1Gb1UhJ77N+W32VdOOXbiS4cophQkfdNhjk"
    "jVunw8YkO7dsBhVP/8bqLDLw/8NsSAKwlzsoNKbrjVQ/NmHMJ88QkiKwv+E6lidy"
    "3wIDAQAB"))

(def configuration-session-oidc {:configurationTemplate {:service      "session-oidc"
                                                         :instance     instance
                                                         :clientID     "FAKE_CLIENT_ID"
                                                         :clientSecret "MyOIDCClientSecret"
                                                         :authorizeURL "https://authorize.oidc.com/authorize"
                                                         :tokenURL     "https://token.oidc.com/token"
                                                         :publicKey    auth-pubkey}})

(deftest lifecycle

  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))
        session-admin (header session-anon authn-info-header "admin ADMIN USER ANON")
        session-user (header session-anon authn-info-header "user USER ANON")
        session-anon-form (-> session-anon
                              (content-type u/form-urlencoded))

        redirect-uri "https://example.com/webui"]

    ;; get session template so that session resources can be tested
    (let [
          ;;
          ;; create the session template to use for these tests
          ;;
          href (-> session-admin
                   (request session-template-base-uri
                            :request-method :post
                            :body (json/write-str session-template-oidc))
                   (ltu/body->edn)
                   (ltu/is-status 201)
                   (ltu/location))

          template-url (str p/service-context href)

          resp (-> session-anon
                   (request template-url)
                   (ltu/body->edn)
                   (ltu/is-status 200))


          name-attr "name"
          description-attr "description"
          properties-attr {:a "one", :b "two"}

          ;;valid-create {:sessionTemplate (ltu/strip-unwanted-attrs template)}
          href-create {:name            name-attr
                       :description     description-attr
                       :properties      properties-attr
                       :sessionTemplate {:href href}}

          href-create-redirect {:sessionTemplate {:href        href
                                                  :redirectURI redirect-uri}}
          invalid-create (assoc-in href-create [:sessionTemplate :invalid] "BAD")]

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
                   :body (json/write-str href-create))
          (ltu/body->edn)
          (ltu/message-matches #".*missing or incorrect configuration.*")
          (ltu/is-status 500))


      ;; anonymous create must succeed
      (let [
            ;;
            ;; create the session-oidc configuration to use for these tests
            ;;
            cfg-href (-> session-admin
                         (request configuration-base-uri
                                  :request-method :post
                                  :body (json/write-str configuration-session-oidc))
                         (ltu/body->edn)
                         (ltu/is-status 201)
                         (ltu/location))


            public-key (:auth-public-key environ.core/env)
            good-claims {:sub         "OIDC_USER"
                         :email       "user@oidc.example.com"
                         :entitlement ["alpha-entitlement"]
                         :groups      ["/organization/group-1"]
                         :realm       "my-realm"}
            good-token (sign/sign-claims good-claims)
            bad-claims {}
            bad-token (sign/sign-claims bad-claims)]


        (is (= cfg-href (str "configuration/session-oidc-" instance)))

        (let [resp (-> session-anon
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
          (is (re-matches callback-pattern (or uri "")))
          (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri2 "")))
          (is (re-matches callback-pattern (or uri2 "")))
          (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri3 "")))
          (is (re-matches callback-pattern (or uri3 "")))
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
              (ltu/is-operation-absent "edit"))
          (-> session-user
              (header authn-info-header (str "user USER ANON " id2))
              (request abs-uri2)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-id id2)
              (ltu/is-operation-present "delete")
              (ltu/is-operation-absent "edit"))
          (-> session-user
              (header authn-info-header (str "user USER ANON " id3))
              (request abs-uri3)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-id id3)
              (ltu/is-operation-present "delete")
              (ltu/is-operation-absent "edit"))

          ;; check contents of session resource
          (let [{:keys [name description properties] :as body} (-> (ltu/ring-app)
                                                                   session
                                                                   (header authn-info-header (str "user USER " id))
                                                                   (request abs-uri)
                                                                   (ltu/body->edn)
                                                                   :response
                                                                   :body)]
            (is (= name name-attr))
            (is (= description description-attr))
            (is (= properties properties-attr)))

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
          (let [get-redirect-uri (fn [u]
                                   (let [r #".*redirect_uri=(.*)$"]
                                     (second (re-matches r u))))
                get-callback-id (fn [u]
                                  (let [r #".*(callback.*)/execute$"]
                                    (second (re-matches r u))))
                validate-url (get-redirect-uri uri)
                validate-url2 (get-redirect-uri uri2)
                validate-url3 (get-redirect-uri uri3)
                callback-id (get-callback-id validate-url)
                callback-id2 (get-callback-id validate-url2)
                callback-id3 (get-callback-id validate-url3)]

            (-> session-admin
                (request (str p/service-context callback-id))
                (ltu/body->edn)
                (ltu/is-status 200))

            (-> session-admin
                (request (str p/service-context callback-id2))
                (ltu/body->edn)
                (ltu/is-status 200))

            (-> session-admin
                (request (str p/service-context callback-id3))
                (ltu/body->edn)
                (ltu/is-status 200))



            ;; remove the authentication configurations
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
                (ltu/is-status 303))                        ;; always expect redirect when redirectURI is provided

            (-> session-anon
                (request validate-url3
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*missing or incorrect configuration.*")
                (ltu/is-status 303))                        ;; always expect redirect when redirectURI is provided

            ;; add the configurations back again
            (-> session-admin
                (request configuration-base-uri
                         :request-method :post
                         :body (json/write-str configuration-session-oidc))
                (ltu/body->edn)
                (ltu/is-status 201))


            ;; try hitting the callback without the OIDC code parameter
            (reset-callback! callback-id)
            (-> session-anon
                (request validate-url
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*not contain required code.*")
                (ltu/is-status 400))

            (reset-callback! callback-id2)
            (-> session-anon
                (request validate-url2
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*not contain required code.*")
                (ltu/is-status 303))                        ;; always expect redirect when redirectURI is provided

            (reset-callback! callback-id3)
            (-> session-anon
                (request validate-url3
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*not contain required code.*")
                (ltu/is-status 303))                        ;; always expect redirect when redirectURI is provided

            ;; try now with a fake code
            (with-redefs [auth-oidc/get-access-token (fn [client-id client-secret token-url oauth-code redirect-url]
                                                       (case oauth-code
                                                         "GOOD" good-token
                                                         "BAD" bad-token
                                                         nil))
                          db/find-roles-for-username (fn [username]
                                                       "USER ANON alpha")
                          db/user-exists? (constantly true)]


              (reset-callback! callback-id)
              (-> session-anon
                  (request (str validate-url "?code=NONE")
                           :request-method :get)
                  (ltu/body->edn)
                  (ltu/message-matches #".*unable to retrieve OIDC/MITREid access token.*")
                  (ltu/is-status 400))

              (reset-callback! callback-id)
              (-> session-anon
                  (request (str validate-url "?code=BAD")
                           :request-method :get)
                  (ltu/body->edn)
                  (ltu/message-matches #".*OIDC/MITREid token is missing subject.*")
                  (ltu/is-status 400))

              (let [_ (reset-callback! callback-id)
                    ring-info (-> session-anon
                                  (request (str validate-url "?code=GOOD")
                                           :request-method :get)
                                  (ltu/body->edn)
                                  (ltu/is-status 400))])    ;; inactive account

              (let [_ (reset-callback! callback-id2)
                    ring-info (-> session-anon
                                  (request (str validate-url2 "?code=GOOD")
                                           :request-method :get)
                                  (ltu/body->edn)
                                  (ltu/is-status 303))
                    location (ltu/location ring-info)
                    token (get-in ring-info [:response :cookies "com.sixsq.slipstream.cookie" :value :token])
                    claims (if token (sign/unsign-claims token) {})]
                #_(is (clojure.string/starts-with? location redirect-uri))
                (is (empty? claims)))

              (let [_ (reset-callback! callback-id3)
                    ring-info (-> session-anon
                                  (request (str validate-url3 "?code=GOOD")
                                           :request-method :get)
                                  (ltu/body->edn)
                                  (ltu/is-status 303))
                    location (ltu/location ring-info)
                    token (get-in ring-info [:response :cookies "com.sixsq.slipstream.cookie" :value :token])
                    claims (if token (sign/unsign-claims token) {})]
                #_(is (clojure.string/starts-with? location redirect-uri))
                (is (empty? claims)))))


          (let [ring-info (-> session-user
                              (header authn-info-header (str "user USER ANON " id))
                              (request abs-uri)
                              (ltu/body->edn)
                              (ltu/is-status 200)
                              (ltu/is-id id)
                              (ltu/is-operation-present "delete")
                              (ltu/is-operation-absent "edit"))
                session (get-in ring-info [:response :body])]
            (is (nil? (:username session)))
            (is (= (:created session) (:updated session))))

          (let [ring-info (-> session-user
                              (header authn-info-header (str "user USER ANON " id2))
                              (request abs-uri2)
                              (ltu/body->edn)
                              (ltu/is-status 200)
                              (ltu/is-id id2)
                              (ltu/is-operation-present "delete")
                              (ltu/is-operation-absent "edit"))
                session (get-in ring-info [:response :body])]
            (is (nil? (:username session)))
            (is (= (:created session) (:updated session))))

          (let [ring-info (-> session-user
                              (header authn-info-header (str "user USER ANON " id3))
                              (request abs-uri3)
                              (ltu/body->edn)
                              (ltu/is-status 200)
                              (ltu/is-id id3)
                              (ltu/is-operation-present "delete")
                              (ltu/is-operation-absent "edit"))
                session (get-in ring-info [:response :body])]
            (is (nil? (:username session)))
            (is (= (:created session) (:updated session))))

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
              (ltu/is-status 400)))))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id session/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :put]
                            [resource-uri :post]])))
