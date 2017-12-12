(ns com.sixsq.slipstream.ssclj.resources.credential-api-key-lifecycle-test
  (:require
    [clojure.test :refer [deftest is are use-fixtures]]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.credential :as credential]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.credential-api-key :as t]
    [com.sixsq.slipstream.ssclj.resources.credential-template-api-key :as akey]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential.key-utils :as key-utils]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase credential/resource-url)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in CredentialTemplate resources
(dyn/initialize)

(deftest check-strip-session-role
  (is (= ["alpha" "beta"] (t/strip-session-role ["alpha" "session/2d273461-2778-4a66-9017-668f6fed43ae" "beta"])))
  (is (= [] (t/strip-session-role ["session/2d273461-2778-4a66-9017-668f6fed43ae"]))))

(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(deftest lifecycle
  (let [session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "root ADMIN USER ANON"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))

        name-attr "name"
        description-attr "description"
        properties-attr {:a "one", :b "two"}

        href (str ct/resource-url "/" akey/method)
        template-url (str p/service-context ct/resource-url "/" akey/method)

        template (-> session-admin
                     (request template-url)
                     (ltu/body->edn)
                     (ltu/is-status 200)
                     (get-in [:response :body]))

        create-import-no-href {:credentialTemplate (strip-unwanted-attrs template)}

        create-import-href {:name name-attr
                            :description description-attr
                            :properties properties-attr
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
          (ltu/is-status 200)))))


