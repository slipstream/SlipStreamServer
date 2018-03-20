(ns com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test-utils
  (:require
    [clojure.test :refer [is]]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.connector :as c]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as cont]
    [com.sixsq.slipstream.ssclj.resources.connector-template-alpha-example :as con-alpha]
    [com.sixsq.slipstream.ssclj.resources.credential :as cred]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as credt]
    [com.sixsq.slipstream.ssclj.resources.credential-template-cloud-alpha :as cred-alpha]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]))


(def ^:const user-info-header "jane USER")
(def ^:const admin-info-header "root ADMIN")

(def obj-store-endpoint "https://s3.cloud.com")
(def connector-name "connector-name")

(defn build-session
  [identity]
  (header (-> (ltu/ring-app)
              session
              (content-type "application/json")) authn-info-header identity))

(def session-admin (build-session admin-info-header))
(def session-user (build-session user-info-header))

(def ^:dynamic *cred-uri* nil)

(defn create-cloud-cred-fixture!
  [f]
  (let [cred-create {:credentialTemplate
                     {:href      (str credt/resource-url "/" cred-alpha/method)
                      :key       "key"
                      :secret    "secret"
                      :quota     7
                      :connector {:href (str c/resource-url "/" connector-name)}}}
        uri         (-> session-user
                        (request (str p/service-context (u/de-camelcase cred/resource-name))
                                 :request-method :post
                                 :body (json/write-str cred-create))
                        (ltu/body->edn)
                        (ltu/is-status 201)
                        (ltu/location))]
    (alter-var-root #'*cred-uri* (constantly uri))
    (f)))

(defn create-connector-fixture!
  [f]
  (let [con-create {:connectorTemplate {:href                (str cont/resource-url "/" con-alpha/cloud-service-type)
                                        :alphaKey            1234
                                        :instanceName        connector-name
                                        :objectStoreEndpoint obj-store-endpoint}}]
    (-> session-admin
        (request (str p/service-context (u/de-camelcase c/resource-name))
                 :request-method :post
                 :body (json/write-str con-create))
        (ltu/body->edn)
        (ltu/is-status 201)
        (ltu/location))
    (f)))

(def base-uri (str p/service-context (u/de-camelcase eo/resource-name)))


;;; Tests.

(defn lifecycle
  [template-url template-data]
  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-user (header session-anon authn-info-header user-info-header)
        session-admin (header session-anon authn-info-header admin-info-header)

        resp (-> session-admin
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))
        template (get-in resp [:response :body])

        valid-create {:externalObjectTemplate (merge (ltu/strip-unwanted-attrs template) template-data)}

        invalid-create (assoc-in valid-create [:externalObjectTemplate :invalid] "BAD")]

    ;; anonymous create should fail if
    ;; 1. 400 he doesn't have access to the credential
    ;; 2. 403 no credentials is in valid-create
    (let [status (-> session-anon
                     (request base-uri
                              :request-method :post
                              :body (json/write-str valid-create))
                     (ltu/body->edn)
                     (get-in [:response :status]))]
      (is (some #(= status %) #{400 403})))

    ;; admin create with invalid template fails
    (-> session-admin
        (request base-uri
                 :request-method :post
                 :body (json/write-str invalid-create))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; full external object lifecycle as administrator/user should work
    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))

          uri-user (-> session-user
                       (request base-uri
                                :request-method :post
                                :body (json/write-str valid-create))
                       (ltu/body->edn)
                       (ltu/is-status 201)
                       (ltu/location))

          abs-uri-user (str p/service-context (u/de-camelcase uri-user))]

      ;; admin get succeeds
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; user query fails
      (-> session-user
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 403))

      ;; anonymous query fails
      (-> session-anon
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 403))

      ;; admin query succeeds
      (let [entries (-> session-admin
                        (request base-uri)
                        (ltu/body->edn)
                        (ltu/is-status 200)
                        (ltu/is-resource-uri eo/collection-uri)
                        (ltu/is-count #(= 2 %))
                        (ltu/entries eo/resource-tag))
            ids (set (map :id entries))]
        (is (= ids #{uri uri-user}))

        ;; verify that all entries are accessible
        (let [pair-fn (juxt :id #(str p/service-context (:id %)))
              pairs (map pair-fn entries)]
          (doseq [[id entry-uri] pairs]
            (-> session-admin
                (request entry-uri)
                (ltu/body->edn)
                (ltu/is-operation-present "delete")
                (ltu/is-operation-present "upload")
                (ltu/is-operation-absent "download")
                (ltu/is-operation-absent "edit")
                (ltu/is-status 200)
                (ltu/is-id id)))))

      ;; admin delete succeeds
      (doseq [uri [abs-uri abs-uri-user]]
        (-> session-admin
            (request uri
                     :request-method :delete
                     :body (json/write-str {:keep-s3-object true})) ;;no s3 deletion while testing
            (ltu/body->edn)
            (ltu/is-status 200)))

      ;; ensure entry is really gone
      (doseq [uri [abs-uri abs-uri-user]]
        (-> session-admin
            (request uri)
            (ltu/body->edn)
            (ltu/is-status 404))))))


(defn upload-and-download-operations
  [template-url template-data]
  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-user (header session-anon authn-info-header user-info-header)
        session-admin (header session-anon authn-info-header admin-info-header)

        resp (-> session-admin
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))

        template (get-in resp [:response :body])

        valid-create {:externalObjectTemplate (merge (ltu/strip-unwanted-attrs template) template-data)}]

    ;; anonymous query is not authorized
    (-> session-anon
        (request template-url)
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; user query is authorized
    (-> session-user
        (request template-url)
        (ltu/body->edn)
        (ltu/is-status 200))

    ;; anonymous create should fail if
    ;; 1. 400 he doesn't have access to the credential
    ;; 2. 403 no credentials is in valid-create
    (let [status (-> session-anon
                     (request base-uri
                              :request-method :post
                              :body (json/write-str valid-create))
                     (ltu/body->edn)
                     (get-in [:response :status]))]
      (is (some #(= status %) #{400 403})))

    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))

          ;; user create should work
          uri-user (-> session-user
                       (request base-uri
                                :request-method :post
                                :body (json/write-str valid-create))
                       (ltu/body->edn)
                       (ltu/is-status 201)
                       (ltu/location))

          abs-uri (str p/service-context (u/de-camelcase uri))

          abs-uri-user (str p/service-context (u/de-camelcase uri-user))

          upload-op (-> session-admin
                        (request abs-uri)
                        (ltu/body->edn)
                        (ltu/is-operation-present "upload")
                        (ltu/is-operation-absent "download")
                        (ltu/is-status 200)
                        (ltu/get-op "upload"))

          upload-op-user (-> session-user
                             (request abs-uri-user)
                             (ltu/body->edn)
                             (ltu/is-operation-present "upload")
                             (ltu/is-operation-absent "download")
                             (ltu/is-status 200)
                             (ltu/get-op "upload"))

          abs-upload-uri (str p/service-context (u/de-camelcase upload-op))

          abs-upload-uri-user (str p/service-context (u/de-camelcase upload-op-user))]

      (-> session-anon
          (request abs-upload-uri
                   :request-method :post)
          (ltu/body->edn)
          (ltu/is-status 403))

      (-> session-user
          (request abs-upload-uri
                   :request-method :post)
          (ltu/body->edn)
          (ltu/is-status 403))

      ;; getting the URL should work
      (is (-> session-admin
              (request abs-upload-uri
                       :request-method :post)
              (ltu/body->edn)
              (ltu/is-status 200)
              :response
              :body
              :uri))

      ;; doing it again should fail because the object should
      ;; be in a 'ready' state
      (-> session-admin
          (request abs-upload-uri
                   :request-method :post)
          (ltu/body->edn)
          (ltu/is-status 400))

      (let [updated-eo (-> session-admin
                           (request abs-uri)
                           (ltu/body->edn)
                           (ltu/is-operation-absent "upload")
                           (ltu/is-operation-present "download")
                           (ltu/is-status 200))
            download-url-action (str p/service-context (ltu/get-op updated-eo "download"))
            state (-> updated-eo :response :body :state)]
        (= eo/state-ready state)
        (is download-url-action)
        (is (-> session-admin
                (request download-url-action
                         :request-method :post)
                (ltu/body->edn)
                (ltu/is-status 200)
                :response
                :body
                :uri)))

      ;; getting the URL should work
      (is (-> session-user
              (request abs-upload-uri-user
                       :request-method :post)
              (ltu/body->edn)
              (ltu/is-status 200)
              :response
              :body
              :uri))

      ;; doing it again should fail because the object should
      ;; be in a 'ready' state
      (-> session-user
          (request abs-upload-uri-user
                   :request-method :post)
          (ltu/body->edn)
          (ltu/is-status 400))

      (let [updated-eo (-> session-user
                           (request abs-uri-user)
                           (ltu/body->edn)
                           (ltu/is-operation-absent "upload")
                           (ltu/is-operation-present "download")
                           (ltu/is-status 200))
            download-url-action (str p/service-context (ltu/get-op updated-eo "download"))
            state (-> updated-eo :response :body :state)]
        (= eo/state-ready state)
        (is download-url-action)
        (is (-> session-user
                (request download-url-action
                         :request-method :post)
                (ltu/body->edn)
                (ltu/is-status 200)
                :response
                :body
                :uri))))))
