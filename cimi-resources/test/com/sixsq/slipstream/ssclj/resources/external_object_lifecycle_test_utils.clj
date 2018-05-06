(ns com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test-utils
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [is]]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.connector :as c]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as cont]
    [com.sixsq.slipstream.ssclj.resources.connector-template-alpha-example :as con-alpha]
    [com.sixsq.slipstream.ssclj.resources.credential :as cred]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as credt]
    [com.sixsq.slipstream.ssclj.resources.credential-template-cloud-alpha :as cred-alpha]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as s3]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [peridot.core :refer :all]))


(def ^:const user-info-header "jane USER")
(def ^:const admin-info-header "root ADMIN")
(def ^:const user-creds-info-header "creds USER")

(def obj-store-endpoint "https://s3.cloud.com")
(def connector-name "connector-name")

(defn build-session
  [identity]
  (header (-> (ltu/ring-app)
              session
              (content-type "application/json")) authn-info-header identity))

(def session-admin (build-session admin-info-header))
(def session-user (build-session user-info-header))
(def session-user-creds (build-session user-creds-info-header))

(def ^:dynamic *cred-uri* nil)

(defn create-cloud-cred
  [user-session]
  (let [cred-create {:credentialTemplate
                     {:href      (str credt/resource-url "/" cred-alpha/method)
                      :key       "key"
                      :secret    "secret"
                      :quota     7
                      :connector {:href (str c/resource-url "/" connector-name)}}}
        uri (-> user-session
                (request (str p/service-context (u/de-camelcase cred/resource-name))
                         :request-method :post
                         :body (json/write-str cred-create))
                (ltu/body->edn)
                (ltu/is-status 201)
                (ltu/location))]
    (alter-var-root #'*cred-uri* (constantly uri))))

(defn create-cloud-cred-fixture-other-user!
  [f]
  (create-cloud-cred session-user-creds)
  (f))

(defn create-cloud-cred-fixture!
  [f]
  (create-cloud-cred session-user)
  (f))

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

(defn create-bucket!
  [obj-store-conf bucket]
  (log/debug (format "TEST. Creating bucket: %s %s" obj-store-conf bucket)))

(defn delete-s3-object
  [obj-store-conf bucket obj-name]
  (log/debug (format "TEST. Deleting s3 object: %s %s %s" obj-store-conf bucket obj-name)))

(defn s3-redefs!
  [f]
  (with-redefs [s3/create-bucket! create-bucket!
                s3/delete-s3-object delete-s3-object]
    (f)))

(def base-uri (str p/service-context (u/de-camelcase eo/resource-name)))


(def session-anon (-> (ltu/ring-app)
                      session
                      (content-type "application/json")))
(def session-user (header session-anon authn-info-header user-info-header))
(def session-admin (header session-anon authn-info-header admin-info-header))

(defn get-template
  [template-url]
  (-> session-admin
      (request template-url)
      (ltu/body->edn)
      (ltu/is-status 200)
      (get-in [:response :body])))

;;; Tests.

(defn test-create-href
  [template-url user-resource]
  (let [template (get-template template-url)
        tmpl-href (:id template)
        valid-create-href {:externalObjectTemplate (merge {:href tmpl-href} user-resource)}
        uri (-> session-user
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-create-href))
                (ltu/body->edn)
                (ltu/is-status 201)
                (ltu/location))
        abs-uri (str p/service-context (u/de-camelcase uri))]
    (-> session-user
        (request abs-uri
                 :request-method :delete)
        (ltu/body->edn)
        (ltu/is-status 200))))


(defn lifecycle-object-type
  [template-url template-obj-1 template-obj-2]
  (let [template (get-template template-url)

        valid-create-1 {:externalObjectTemplate (merge (ltu/strip-unwanted-attrs template) template-obj-1)}
        valid-create-2 {:externalObjectTemplate (merge (ltu/strip-unwanted-attrs template) template-obj-2)}

        invalid-create (assoc-in valid-create-1 [:externalObjectTemplate :invalid] "BAD")]

    ;; anonymous create should fail if
    ;; 1. 400 he doesn't have access to the credential
    ;; 2. 403 no credentials is in valid-create
    (let [status (-> session-anon
                     (request base-uri
                              :request-method :post
                              :body (json/write-str valid-create-1))
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

    ;; it is not possible to create the same external object twice
    (let [uri (-> session-user
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-create-1))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      (-> session-user
          (request base-uri
                   :request-method :post
                   :body (json/write-str valid-create-1))
          (ltu/body->edn)
          (ltu/is-status 409))

      ;; cleanup
      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))

    ;; full external object lifecycle as administrator/user should work
    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-create-1))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))

          uri-user (-> session-user
                       (request base-uri
                                :request-method :post
                                :body (json/write-str valid-create-2))
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
                (ltu/is-key-value :state eo/state-new)
                (ltu/is-operation-present "delete")
                (ltu/is-operation-present "upload")
                (ltu/is-operation-absent "ready")
                (ltu/is-operation-absent "download")
                (ltu/is-operation-absent "edit")
                (ltu/is-status 200)))))

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
  [template-url template-obj-1 template-obj-2]
  (let [template (get-template template-url)

        valid-create-1 {:externalObjectTemplate (merge (ltu/strip-unwanted-attrs template) template-obj-1)}
        valid-create-2 {:externalObjectTemplate (merge (ltu/strip-unwanted-attrs template) template-obj-2)}]

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
                              :body (json/write-str valid-create-1))
                     (ltu/body->edn)
                     (get-in [:response :status]))]
      (is (some #(= status %) #{400 403})))

    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-create-1))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))

          ;; user create of a different object should work
          uri-user (-> session-user
                       (request base-uri
                                :request-method :post
                                :body (json/write-str valid-create-2))
                       (ltu/body->edn)
                       (ltu/is-status 201)
                       (ltu/location))

          abs-uri (str p/service-context (u/de-camelcase uri))

          abs-uri-user (str p/service-context (u/de-camelcase uri-user))

          upload-op (-> session-admin
                        (request abs-uri)
                        (ltu/body->edn)
                        (ltu/is-operation-present "upload")
                        (ltu/is-operation-present "delete")
                        (ltu/is-operation-absent "ready")
                        (ltu/is-operation-absent "download")
                        (ltu/is-status 200)
                        (ltu/get-op "upload"))

          upload-op-user (-> session-user
                             (request abs-uri-user)
                             (ltu/body->edn)
                             (ltu/is-operation-present "upload")
                             (ltu/is-operation-present "delete")
                             (ltu/is-operation-absent "ready")
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
              (ltu/is-status 200)))

      ;; after getting upload URL the state is set to 'uploading' and only 'ready' action is present
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-key-value :state eo/state-uploading)
          (ltu/is-operation-present "ready")
          (ltu/is-operation-present "delete")
          (ltu/is-operation-present "upload")
          (ltu/is-operation-absent "download")
          (ltu/is-status 200))

      ;; doing it again should fail because the object is in 'uploading' state
      (-> session-admin
          (request abs-upload-uri
                   :request-method :post)
          (ltu/body->edn)
          (ltu/is-status 400))

      (let [uploading-eo (-> session-admin
                             (request abs-uri)
                             (ltu/body->edn)
                             (ltu/is-key-value :state eo/state-uploading)
                             (ltu/is-operation-present "delete")
                             (ltu/is-operation-present "ready")
                             (ltu/is-operation-present "upload")
                             (ltu/is-operation-absent "download")
                             (ltu/is-status 200))
            ;; after upload is done, change object state to 'ready'
            ready-url-action (str p/service-context (ltu/get-op uploading-eo "ready"))
            _ (-> session-admin
                  (request ready-url-action
                           :request-method :post)
                  (ltu/body->edn)
                  (ltu/is-status 200))
            ready-eo (-> session-admin
                         (request abs-uri)
                         (ltu/body->edn)
                         (ltu/is-key-value :state eo/state-ready)
                         (ltu/is-operation-present "download")
                         (ltu/is-operation-present "delete")
                         (ltu/is-operation-absent "upload")
                         (ltu/is-operation-absent "ready")
                         (ltu/is-status 200))
            download-url-action (str p/service-context (ltu/get-op ready-eo "download"))]
        (is (-> session-admin
                (request download-url-action
                         :request-method :post)
                (ltu/body->edn)
                (ltu/is-status 200))))

      ;; getting the URL should work
      (is (-> session-user
              (request abs-upload-uri-user
                       :request-method :post)
              (ltu/body->edn)
              (ltu/is-status 200)))

      ;; after getting upload URL the state is set to 'uploading' and only 'ready' action is present
      (-> session-user
          (request abs-uri-user)
          (ltu/body->edn)
          (ltu/is-key-value :state eo/state-uploading)
          (ltu/is-operation-present "delete")
          (ltu/is-operation-present "ready")
          (ltu/is-operation-present "upload")
          (ltu/is-operation-absent "download")
          (ltu/is-status 200))

      ;; doing it again should fail because the object is in 'uploading' state
      (-> session-user
          (request abs-upload-uri-user
                   :request-method :post)
          (ltu/body->edn)
          (ltu/is-status 400))
      (let [uploading-eo (-> session-user
                             (request abs-uri-user)
                             (ltu/body->edn)
                             (ltu/is-key-value :state eo/state-uploading)
                             (ltu/is-operation-present "delete")
                             (ltu/is-operation-present "ready")
                             (ltu/is-operation-present "upload")
                             (ltu/is-operation-absent "download")
                             (ltu/is-status 200))
            ;; after upload is done, change object state to 'ready'
            ready-url-action (str p/service-context (ltu/get-op uploading-eo "ready"))
            _ (-> session-user
                  (request ready-url-action
                           :request-method :post)
                  (ltu/body->edn)
                  (ltu/is-status 200))
            ready-eo (-> session-user
                         (request abs-uri-user)
                         (ltu/body->edn)
                         (ltu/is-key-value :state eo/state-ready)
                         (ltu/is-operation-present "delete")
                         (ltu/is-operation-present "download")
                         (ltu/is-operation-absent "upload")
                         (ltu/is-operation-absent "ready")
                         (ltu/is-status 200))
            download-url-action (str p/service-context (ltu/get-op ready-eo "download"))]
        (is (-> session-user
                (request download-url-action
                         :request-method :post)
                (ltu/body->edn)
                (ltu/is-status 200)))))))
