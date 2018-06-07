(ns com.sixsq.slipstream.ssclj.resources.user-oidc-registration-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.oidc :as auth-oidc]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as cbu]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as configuration]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.user :as user]
    [com.sixsq.slipstream.ssclj.resources.user-template :as ut]
    [com.sixsq.slipstream.ssclj.resources.user-template-oidc-registration :as oidc]
    [peridot.core :refer :all]
    [ring.util.codec :as codec]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase user/resource-name)))

(def configuration-base-uri (str p/service-context (u/de-camelcase configuration/resource-name)))

(def user-template-base-uri (str p/service-context (u/de-camelcase ut/resource-name)))

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

(def configuration-user-oidc {:configurationTemplate {:service   "session-oidc" ;;reusing configuration from session OIDC
                                                      :instance  oidc/registration-method
                                                      :clientID  "FAKE_CLIENT_ID"
                                                      :baseURL   "https://oidc.example.com"
                                                      :publicKey auth-pubkey}})

(deftest lifecycle

  (let [

        href (str ut/resource-url "/" oidc/registration-method)
        template-url (str p/service-context ut/resource-url "/" oidc/registration-method)

        session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))
        session-admin (header session-anon authn-info-header "admin ADMIN USER ANON")
        session-user (header session-anon authn-info-header "user USER ANON")
        session-anon-form (-> session-anon
                              (content-type user/form-urlencoded))

        redirect-uri "https://example.com/webui"]

    ;; get user template so that user resources can be tested
    (let [template (-> session-admin
                       (request template-url)
                       (ltu/body->edn)
                       (ltu/is-status 200)
                       (get-in [:response :body]))

          name-attr "name"
          description-attr "description"
          properties-attr {:a "one", :b "two"}

          href-create {:name         name-attr
                       :description  description-attr
                       :properties   properties-attr
                       :userTemplate {:href href}}
          href-create-redirect {:userTemplate {:href        href
                                               :redirectURI redirect-uri}}
          invalid-create (assoc-in href-create [:userTemplate :invalid] "BAD")]

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

      ;; anonymous create must succeed (normal create and href create)
      (let [
            ;;
            ;; create the session-oidc configuration to use for these tests
            ;;
            cfg-href (-> session-admin
                         (request configuration-base-uri
                                  :request-method :post
                                  :body (json/write-str configuration-user-oidc))
                         (ltu/body->edn)
                         (ltu/is-status 201)
                         (ltu/location))

            public-key (:auth-public-key environ.core/env)
            good-claims {:sub         "OIDC_USER"
                         :email       "user@oidc.example.com"
                         :given_name  "John"
                         :family_name "Smith"
                         :entitlement ["alpha-entitlement"]
                         :groups      ["/organization/group-1"]
                         :realm       "my-realm"}
            good-token (sign/sign-claims good-claims)
            bad-claims {}
            bad-token (sign/sign-claims bad-claims)]

        (is (= cfg-href (str "configuration/session-oidc-" oidc/registration-method)))

        (let [uri (-> session-anon
                      (request base-uri
                               :request-method :post
                               :body (json/write-str href-create))
                      (ltu/body->edn)
                      (ltu/is-status 303)
                      ltu/location)

              uri2 (-> session-anon
                       (request base-uri
                                :request-method :post
                                :body (json/write-str href-create-redirect))
                       (ltu/body->edn)
                       (ltu/is-status 303)
                       ltu/location)

              uri3 (-> session-anon-form
                       (request base-uri
                                :request-method :post
                                :body (codec/form-encode {:href        href
                                                          :redirectURI redirect-uri}))
                       (ltu/body->edn)
                       (ltu/is-status 303)
                       ltu/location)]

          ;; redirect URLs in location header should contain the client ID and resource id
          (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri "")))
          (is (re-matches callback-pattern (or uri "")))
          (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri2 "")))
          (is (re-matches callback-pattern (or uri2 "")))
          (is (re-matches #".*FAKE_CLIENT_ID.*" (or uri3 "")))
          (is (re-matches callback-pattern (or uri3 "")))

          ;; anonymous query should succeed but still have no users
          (-> session-anon
              (request base-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-count zero?))

          ;; user query should succeed but have no users
          (-> session-user
              (request base-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-count zero?))

          ;; admin query should succeed, but see no users
          (-> session-admin
              (request base-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-count 0))

          ;; validate callbacks
          (let [get-redirect-uri #(->> % (re-matches #".*redirect_uri=(.*)$") second)
                get-callback-id #(->> % (re-matches #".*(callback.*)/execute$") second)

                validate-url (get-redirect-uri uri)
                validate-url2 (get-redirect-uri uri2)
                validate-url3 (get-redirect-uri uri3)

                callback-id (get-callback-id validate-url)
                callback-id2 (get-callback-id validate-url2)
                callback-id3 (get-callback-id validate-url3)]

            ;; all callbacks must exist
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
                (ltu/is-status 500))                        ;; FIXME: always expect redirect 303 when redirectURI is provided

            (-> session-anon
                (request validate-url3
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*missing or incorrect configuration.*")
                (ltu/is-status 500))                        ;; FIXME: always expect redirect 303 when redirectURI is provided

            ;; add the configuration back again
            (-> session-admin
                (request configuration-base-uri
                         :request-method :post
                         :body (json/write-str configuration-user-oidc))
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
                (ltu/is-status 400))                        ;; FIXME: always expect redirect 303 when redirectURI is provided

            (reset-callback! callback-id3)
            (-> session-anon
                (request validate-url3
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*not contain required code.*")
                (ltu/is-status 400))                        ;; FIXME: always expect redirect 303 when redirectURI is provided

            ;; try now with a fake code
            (with-redefs [auth-oidc/get-oidc-access-token (fn [client-id client-secret oauth-code redirect-url]
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
                  (ltu/message-matches #".*unable to retrieve OIDC access token.*")
                  (ltu/is-status 400))

              (is (= "FAILED" (-> session-admin
                                  (request (str p/service-context callback-id))
                                  (ltu/body->edn)
                                  (ltu/is-status 200)
                                  :response
                                  :body
                                  :state)))

              (reset-callback! callback-id)
              (-> session-anon
                  (request (str validate-url "?code=BAD")
                           :request-method :get)
                  (ltu/body->edn)
                  (ltu/message-matches #".*OIDC token is missing subject.*")
                  (ltu/is-status 400))

              (is (= "FAILED" (-> session-admin
                                  (request (str p/service-context callback-id))
                                  (ltu/body->edn)
                                  (ltu/is-status 200)
                                  :response
                                  :body
                                  :state)))

              (reset-callback! callback-id)
              (-> session-anon
                  (request (str validate-url "?code=GOOD")
                           :request-method :get)
                  (ltu/is-status 200))

              #_(reset-callback! callback-id2)
              #_(let [ring-info (-> session-anon
                                    (request (str validate-url2 "?code=GOOD")
                                             :request-method :get)
                                    (ltu/body->edn)
                                    (ltu/is-status 303))
                      location (ltu/location ring-info)
                      token (get-in ring-info [:response :cookies "com.sixsq.slipstream.cookie" :value :token])
                      claims (if token (sign/unsign-claims token) {})]
                  #_(is (clojure.string/starts-with? location redirect-uri))
                  (is (empty? claims)))

              #_(reset-callback! callback-id3)
              #_(let [ring-info (-> session-anon
                                    (request (str validate-url3 "?code=GOOD")
                                             :request-method :get)
                                    (ltu/body->edn)
                                    (ltu/is-status 303))
                      location (ltu/location ring-info)
                      token (get-in ring-info [:response :cookies "com.sixsq.slipstream.cookie" :value :token])
                      claims (if token (sign/unsign-claims token) {})]
                  #_(is (clojure.string/starts-with? location redirect-uri))
                  (is (empty? claims)))))


          #_(let [ring-info (-> session-user
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

          #_(let [ring-info (-> session-user
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

          #_(let [ring-info (-> session-user
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
          #_(-> session-user
                (header authn-info-header (str "user USER ANON " id))
                (request abs-uri
                         :request-method :delete)
                (ltu/is-unset-cookie)
                (ltu/body->edn)
                (ltu/is-status 200))
          #_(-> session-user
                (header authn-info-header (str "user USER ANON " id2))
                (request abs-uri2
                         :request-method :delete)
                (ltu/is-unset-cookie)
                (ltu/body->edn)
                (ltu/is-status 200))
          #_(-> session-user
                (header authn-info-header (str "user USER ANON " id3))
                (request abs-uri3
                         :request-method :delete)
                (ltu/is-unset-cookie)
                (ltu/body->edn)
                (ltu/is-status 200))

          ;; create with invalid template fails
          #_(-> session-anon
                (request base-uri
                         :request-method :post
                         :body (json/write-str invalid-create))
                (ltu/body->edn)
                (ltu/is-status 400)))))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id user/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :put]
                            [resource-uri :post]])))


