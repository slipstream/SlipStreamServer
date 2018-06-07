(ns com.sixsq.slipstream.ssclj.resources.user-github-registration-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.auth.github :as auth-github]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as cbu]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as configuration]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.user :as user]
    [com.sixsq.slipstream.ssclj.resources.user-template :as ut]
    [com.sixsq.slipstream.ssclj.resources.user-template-github-registration :as utg]
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


(def configuration-user-github {:configurationTemplate {:service      "session-github" ;;reusing configuration from session GitHub
                                                        :instance     utg/registration-method

                                                        :clientID     "FAKE_CLIENT_ID"
                                                        :clientSecret "ABCDEF..."}})

(deftest lifecycle

  (let [

        href (str ut/resource-url "/" utg/registration-method)
        template-url (str p/service-context ut/resource-url "/" utg/registration-method)

        session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))
        session-admin (header session-anon authn-info-header "admin ADMIN USER ANON")
        session-user (header session-anon authn-info-header "user USER ANON")
        session-anon-form (-> session-anon
                              (content-type user/form-urlencoded))

        redirect-uri-example "https://example.com/webui"]

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
                                               :redirectURI redirect-uri-example}}
          invalid-create (assoc-in href-create [:userTemplate :invalid] "BAD")]

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



      (let [
            ;;
            ;; create the session-github configuration to use for these tests
            ;;
            cfg-href (-> session-admin
                         (request configuration-base-uri
                                  :request-method :post
                                  :body (json/write-str configuration-user-github))
                         (ltu/body->edn)
                         (ltu/is-status 201)
                         (ltu/location))

            _ (is (= cfg-href (str "configuration/session-github-" utg/registration-method)))

            resp (-> session-anon
                     (request base-uri
                              :request-method :post
                              :body (json/write-str href-create))
                     (ltu/body->edn)
                     (ltu/is-status 303))

            redirect-uri (-> session-anon
                             (request base-uri
                                      :request-method :post
                                      :body (json/write-str href-create))
                             (ltu/body->edn)
                             (ltu/location))

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
        (doseq [u [uri uri2 uri3]]
          (is (re-matches #".*FAKE_CLIENT_ID.*" (or u "")))
          (is (re-matches callback-pattern (or u ""))))

        ;; anonymous, user and admin query should succeed but have no users
        (doseq [session [session-anon session-user session-admin]]
          (-> session
              (request base-uri)
              (ltu/body->edn)
              (ltu/is-status 200)
              (ltu/is-count zero?)))


        ;;
        ;; test validation callback
        ;;
        (let [get-redirect-uri #(->> % (re-matches #".*redirect_uri=(.*)$") second)
              get-callback-id #(->> % (re-matches #".*(callback.*)/execute$") second)

              validate-url (get-redirect-uri uri)
              validate-url2 (get-redirect-uri uri2)
              validate-url3 (get-redirect-uri uri3)

              callback-id (get-callback-id validate-url)
              callback-id2 (get-callback-id validate-url2)
              callback-id3 (get-callback-id validate-url3)]


          ;; all callbacks must exist
          (doseq [cb-id [callback-id callback-id2 callback-id3]]
            (-> session-admin
                (request (str p/service-context cb-id))
                (ltu/body->edn)
                (ltu/is-status 200)))


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
                       :body (json/write-str configuration-user-github))
              (ltu/body->edn)
              (ltu/is-status 201))


          ;; try hitting the callback without the OAuth code parameter
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
              (ltu/is-status 303))

          (reset-callback! callback-id3)
          (-> session-anon
              (request validate-url3
                       :request-method :get)
              (ltu/body->edn)
              (ltu/message-matches #".*not contain required code.*")
              (ltu/is-status 303))


          ;; try now with a fake code
          (reset-callback! callback-id)

          (with-redefs [auth-github/get-github-access-token (fn [client-id client-secret oauth-code]
                                                              (case oauth-code
                                                                "GOOD" "GOOD_ACCESS_CODE"
                                                                "BAD" "BAD_ACCESS_CODE"
                                                                nil))
                        auth-github/get-github-user-info (fn [access-code]
                                                           (when (= access-code "GOOD_ACCESS_CODE")
                                                             {:login "GITHUB_USER"
                                                              :email "user@example.com"}))

                        ex/match-existing-external-user (fn [authn-method external-login external-email]
                                                          ["MATCHED_USER" "/dashboard"])

                        db/find-roles-for-username (fn [username]
                                                     "USER ANON alpha")]

            (-> session-anon
                (request (str validate-url "?code=NONE")
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*unable to retrieve GitHub access code.*")
                (ltu/is-status 400))

            (reset-callback! callback-id)
            (-> session-anon
                (request (str validate-url "?code=BAD")
                         :request-method :get)
                (ltu/body->edn)
                (ltu/message-matches #".*unable to retrieve GitHub user information.*")
                (ltu/is-status 400))


            (let [_ (reset-callback! callback-id)
                  ring-info (-> session-anon
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

            (let [_ (reset-callback! callback-id2)
                  ring-info (-> session-anon
                                (request (str validate-url2 "?code=GOOD")
                                         :request-method :get)
                                (ltu/body->edn)
                                (ltu/is-status 303)
                                (ltu/is-set-cookie))
                  location (ltu/location ring-info)
                  token (get-in ring-info [:response :cookies "com.sixsq.slipstream.cookie" :value :token])
                  claims (if token (sign/unsign-claims token) {})]
              (is (= location redirect-uri-example))
              (is (= "MATCHED_USER" (:username claims)))
              (is (re-matches (re-pattern (str ".*" id2 ".*")) (or (:roles claims) ""))))

            (let [_ (reset-callback! callback-id3)
                  ring-info (-> session-anon
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


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id user/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :put]
                            [resource-uri :post]])))


