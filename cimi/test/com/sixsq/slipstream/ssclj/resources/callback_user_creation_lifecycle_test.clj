(ns com.sixsq.slipstream.ssclj.resources.callback-user-creation-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback-create-oidc-user :as user]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.auth.cyclone :as auth-oidc]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.resources.configuration :as configuration]
    [com.sixsq.slipstream.auth.utils.sign :as sign]))


(use-fixtures :each ltu/with-test-server-fixture)


(def base-uri (str p/service-context (u/de-camelcase callback/resource-url)))


(def configuration-base-uri (str p/service-context (u/de-camelcase configuration/resource-name)))


(def instance "oidc")


(def auth-pubkey
  (str
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA835H7CQt2oOmlj6GoZp+"
    "dFLE6k43Ybi3ku/yuuzatlnet95xVibbyD+DWBz8owRx5F7dZKbFuJPD7KNZWnxD"
    "P4hSO6p7xg6xOjWrU2naMW8SaWs8cbU7rssRKbEmCc39888pgNi6/VgZiHXmVeUR"
    "eWbxlrppIhIrRiHwf8LHA0LzGn0UAS4K0dMPdRR02vWs5hRw8yOAr0hXU2LUb7AO"
    "uP73cumiWDqkmJBhKa1PYN7vixkud1Gb1UhJ77N+W32VdOOXbiS4cophQkfdNhjk"
    "jVunw8YkO7dsBhVP/8bqLDLw/8NsSAKwlzsoNKbrjVQ/NmHMJ88QkiKwv+E6lidy"
    "3wIDAQAB"))


(def configuration-session-oidc {:configurationTemplate {:service   "session-oidc"
                                                         :instance  instance
                                                         :clientID  "FAKE_CLIENT_ID"
                                                         :baseURL   "https://oidc.example.com"
                                                         :publicKey auth-pubkey}})



(deftest lifecycle
  (let [session (-> (ltu/ring-app)
                    session
                    (content-type "application/json"))
        session-admin (header session authn-info-header "root ADMIN USER ANON")
        session-anon (header session authn-info-header "unknown ANON")


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

        good-claims {:sub         "oidc-user-really-does-not-exist"
                     :email       "user@oidc.example.com"
                     :entitlement ["alpha-entitlement"]
                     :groups      ["/organization/group-1"]
                     :realm       "my-realm"}
        good-token (sign/sign-claims good-claims)
        ]

    (with-redefs [auth-oidc/get-oidc-access-token (fn [client-id client-secret oauth-code redirect-url]
                                                    good-token)]

      ;; create a callback as an admin
      (let [create-callback-succeeds {:action         user/action-name
                                      :targetResource {:href "example/test-user"}
                                      :data           {:key1 "key1"
                                                       :key2 "key2"}}

            uri-succeeds (str p/service-context (-> session-admin
                                                    (request base-uri
                                                             :request-method :post
                                                             :body (json/write-str create-callback-succeeds))
                                                    (ltu/body->edn)
                                                    (ltu/is-status 201)
                                                    :response
                                                    :body
                                                    :resource-id))

            trigger-succeeds (str p/service-context (-> session-admin
                                                        (request uri-succeeds)
                                                        (ltu/body->edn)
                                                        (ltu/is-status 200)
                                                        (ltu/get-op "execute")))

            uri-user (str p/service-context "user/oidc-user-really-does-not-exist")]

        ;; created user should not exist
        (-> session-admin
            (request uri-user)
            (ltu/body->edn)
            (ltu/is-status 404))

        ;; anon should be able to trigger the callbacks
        (-> session-anon
            (request (str trigger-succeeds "?code=SOMETHING"))
            (ltu/body->edn)
            (ltu/is-status 200))

        ;; retriggering the callbacks must fail with 409
        (-> session-anon
            (request (str trigger-succeeds "?code=SOMETHING"))
            (ltu/body->edn)
            (ltu/is-status 409))

        ;; delete
        (-> session-admin
            (request uri-succeeds
                     :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 200))

        ;; ensure that an unknown callback returns 404
        (-> session-anon
            (request trigger-succeeds)
            (ltu/body->edn)
            (ltu/is-status 404))

        ;; created user should now exist!
        (-> session-admin
            (request uri-user)
            (ltu/body->edn)
            (ltu/is-status 200))

        ;; clean up the created user
        (-> session-admin
            (request uri-user
                     :method :delete)
            (ltu/body->edn)
            (ltu/is-status 200))


        ))))
