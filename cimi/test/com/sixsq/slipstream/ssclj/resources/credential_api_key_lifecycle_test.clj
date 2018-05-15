(ns com.sixsq.slipstream.ssclj.resources.credential-api-key-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [are deftest is use-fixtures]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential :as credential]
    [com.sixsq.slipstream.ssclj.resources.credential-api-key :as t]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.credential-template-api-key :as akey]
    [com.sixsq.slipstream.ssclj.resources.credential.key-utils :as key-utils]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [peridot.core :refer :all]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase credential/resource-name)))

(def session-anon (-> (ltu/ring-app)
                      session
                      (content-type "application/json")))
(def session-admin (header session-anon authn-info-header "root ADMIN"))
(def session-user (header session-anon authn-info-header "jane USER ANON"))

(def template-url (str p/service-context ct/resource-url "/" akey/credential-type))


(deftest check-strip-session-role
  (is (= ["alpha" "beta"] (t/strip-session-role ["alpha" "session/2d273461-2778-4a66-9017-668f6fed43ae" "beta"])))
  (is (= [] (t/strip-session-role ["session/2d273461-2778-4a66-9017-668f6fed43ae"]))))

(deftest lifecycle
  (let [name-attr "name"
        description-attr "description"
        properties-attr {:a "one", :b "two"}

        href (str ct/resource-url "/" akey/method)
        template-url (str p/service-context ct/resource-url "/" akey/method)

        template (-> session-admin
                     (request template-url)
                     (ltu/body->edn)
                     (ltu/is-status 200)
                     (get-in [:response :body]))

        create-import-no-href {:credentialTemplate (ltu/strip-unwanted-attrs template)}


        create-import-href {:name               name-attr
                            :description        description-attr
                            :properties         properties-attr
                            :credentialTemplate {:href href
                                                 :ttl  1000}}

        create-import-href-zero-ttl {:credentialTemplate {:href href
                                                          :ttl  0}}

        create-import-href-no-ttl {:credentialTemplate {:href href}}

        invalid-create-href (assoc-in create-import-href [:credentialTemplate :href] "credential-template/unknown-template")]

    ;; admin/user query should succeed but be empty (no credentials created yet)
    (doseq [session [session-admin session-user]]
      (-> session
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-count zero?)
          (ltu/is-operation-present "add")
          (ltu/is-operation-absent "delete")
          (ltu/is-operation-absent "edit")))

    ;; anonymous credential collection query should not succeed
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; creating a new credential without reference will fail for all types of users
    (doseq [session [session-admin session-user session-anon]]
      (-> session
          (request base-uri
                   :request-method :post
                   :body (json/write-str create-import-no-href))
          (ltu/body->edn)
          (ltu/is-status 400)))

    ;; creating a new credential as anon will fail; expect 400 because href cannot be accessed
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str create-import-href))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; create a credential as a normal user
    (let [resp (-> session-user
                   (request base-uri
                            :request-method :post
                            :body (json/write-str create-import-href))
                   (ltu/body->edn)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          secret-key (get-in resp [:response :body :secretKey])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; resource id and the uri (location) should be the same
      (is (= id uri))

      ;; the secret key must be returned as part of the 201 response
      (is secret-key)

      ;; admin/user should be able to see and delete credential
      (doseq [session [session-admin session-user]]
        (-> session
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-operation-present "delete")
            (ltu/is-operation-present "edit")))

      ;; ensure credential contains correct information
      (let [{:keys [name description properties
                    digest expiry claims]} (-> session-user
                                               (request abs-uri)
                                               (ltu/body->edn)
                                               (ltu/is-status 200)
                                               :response
                                               :body)]
        (is (= name name-attr))
        (is (= description description-attr))
        (is (= properties properties-attr))
        (is digest)
        (is (key-utils/valid? secret-key digest))
        (is expiry)
        (is claims))

      ;; delete the credential
      (-> session-user
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))

    ;; execute the same tests but now create an API key without an expiry date
    (let [resp (-> session-user
                   (request base-uri
                            :request-method :post
                            :body (json/write-str create-import-href-no-ttl))
                   (ltu/body->edn)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          secret-key (get-in resp [:response :body :secretKey])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; resource id and the uri (location) should be the same
      (is (= id uri))

      ;; the secret key must be returned as part of the 201 response
      (is secret-key)

      ;; admin/user should be able to see and delete credential
      (doseq [session [session-admin session-user]]
        (-> session
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-operation-present "delete")
            (ltu/is-operation-present "edit")))

      ;; ensure credential contains correct information
      (let [{:keys [digest expiry claims]} (-> session-user
                                               (request abs-uri)
                                               (ltu/body->edn)
                                               (ltu/is-status 200)
                                               :response
                                               :body)]
        (is digest)
        (is (key-utils/valid? secret-key digest))
        (is (nil? expiry))
        (is claims))

      ;; delete the credential
      (-> session-user
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))

    ;; and again, with a zero TTL (should be same as if TTL was not given)
    (let [resp (-> session-user
                   (request base-uri
                            :request-method :post
                            :body (json/write-str create-import-href-zero-ttl))
                   (ltu/body->edn)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          secret-key (get-in resp [:response :body :secretKey])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; resource id and the uri (location) should be the same
      (is (= id uri))

      ;; the secret key must be returned as part of the 201 response
      (is secret-key)

      ;; admin/user should be able to see and delete credential
      (doseq [session [session-admin session-user]]
        (-> session
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-operation-present "delete")
            (ltu/is-operation-present "edit")))

      ;; ensure credential contains correct information
      (let [{:keys [digest expiry claims] :as current} (-> session-user
                                                           (request abs-uri)
                                                           (ltu/body->edn)
                                                           (ltu/is-status 200)
                                                           :response
                                                           :body)]
        (is digest)
        (is (key-utils/valid? secret-key digest))
        (is (nil? expiry))
        (is claims)

        ;; update the credential by changing the name attribute
        (-> session-user
            (request abs-uri
                     :request-method :put
                     :body (json/write-str (assoc current :name "UPDATED!")))
            (ltu/body->edn)
            (ltu/is-status 200))

        ;; verify that the attribute has been changed
        (let [expected (assoc current :name "UPDATED!")
              reread (-> session-user
                         (request abs-uri)
                         (ltu/body->edn)
                         (ltu/is-status 200)
                         :response
                         :body)]

          (is (= (dissoc expected :updated) (dissoc reread :updated)))
          (is (not= (:updated expected) (:updated reread)))))

      ;; delete the credential
      (-> session-user
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))))

(defn get-valid-create-tmpl
  []
  {:name               "name"
   :description        "description"
   :properties         {:a "one", :b "two"}
   :credentialTemplate {:href (str ct/resource-url "/" akey/method)
                        :ttl  1000}})

(deftest disable-operation
  "Check the disable operation"
  (let [valid-create (get-valid-create-tmpl)
        uri (-> session-admin
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-create))
                (ltu/body->edn)
                (ltu/is-status 201)
                (ltu/location))
        abs-uri (str p/service-context (u/de-camelcase uri))
        disable-op (-> session-admin
                       (request abs-uri)
                       (ltu/body->edn)
                       (ltu/is-operation-present "disable")
                       (ltu/is-status 200)
                       (ltu/get-op "disable"))
        abs-disable-uri (str p/service-context (u/de-camelcase disable-op))]

    ;; can not disable credential as anon
    (-> session-anon
        (request abs-disable-uri
                 :request-method :post)
        (ltu/body->edn)
        (ltu/is-status 403))

    ;can not disable credential as user
    (-> session-user
        (request abs-disable-uri
                 :request-method :post)
        (ltu/body->edn)
        (ltu/is-status 403))

    ;enabled should now be set to false
    (-> session-admin
        (request abs-disable-uri
                 :request-method :post)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-key-value :enabled false))

    ;disabling an already disabled should not be possible
    (-> session-admin
        (request abs-disable-uri
                 :request-method :post)
        (ltu/body->edn)
        (ltu/is-status 400))))


