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


(def ^:const user-info-header "jane USER ANON")
(def ^:const admin-info-header "root ADMIN USER ANON")
(def ^:const user-creds-info-header "creds USER ANON")

(def ^:const username-view "tarzan")
(def ^:const user-view-info-header (str username-view " USER ANON"))

(def ^:const username-no-view "other")
(def ^:const user-no-view-info-header (str username-no-view " USER ANON"))

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

(def session-user-view (build-session user-view-info-header))
(def session-user-no-view (build-session user-no-view-info-header))


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
      :response
      :body))


(defn full-eo-lifecycle
  [template-url template-obj]
  (let [template (get-template template-url)
        create-href {:externalObjectTemplate (-> template-obj
                                                 (assoc :href (:id template))
                                                 (dissoc :objectType))}
        create-no-href {:externalObjectTemplate (merge (ltu/strip-unwanted-attrs template) template-obj)}]

    ;; check with and without a href attribute
    (doseq [valid-create [create-href create-no-href]]

      (let [invalid-create (assoc-in valid-create [:externalObjectTemplate :invalid] "BAD")]

        ;; anonymous create should always return a 403 error
        (-> session-anon
            (request base-uri
                     :request-method :post
                     :body (json/write-str valid-create))
            (ltu/body->edn)
            (ltu/is-status 403))

        ;; full external object lifecycle as administrator/user should work
        (doseq [session [session-admin session-user]]

          ;; create with invalid template fails
          (-> session
              (request base-uri
                       :request-method :post
                       :body (json/write-str invalid-create))
              (ltu/body->edn)
              (ltu/is-status 400))

          ;; creating the same object twice is not allowed
          (let [uri (-> session
                        (request base-uri
                                 :request-method :post
                                 :body (json/write-str valid-create))
                        (ltu/body->edn)
                        (ltu/is-status 201)
                        (ltu/location))
                abs-uri (str p/service-context (u/de-camelcase uri))]

            (-> session
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-create))
                (ltu/body->edn)
                (ltu/is-status 409))

            ;; cleanup
            (-> session
                (request abs-uri
                         :request-method :delete)
                (ltu/body->edn)
                (ltu/is-status 200)))

          (let [uri (-> session
                        (request base-uri
                                 :request-method :post
                                 :body (json/write-str valid-create))
                        (ltu/body->edn)
                        (ltu/is-status 201)
                        (ltu/location))
                abs-uri (str p/service-context (u/de-camelcase uri))]

            ;; retrieve works
            (-> session
                (request abs-uri)
                (ltu/body->edn)
                (ltu/is-status 200))

            ;; retrieve by another authorized user succeeds
            #_(-> session-user-view
                  (request abs-uri)
                  (ltu/body->edn)
                  (ltu/is-status 200))

            ;; retrieve by another user fails
            (-> session-user-no-view
                (request abs-uri)
                (ltu/body->edn)
                (ltu/is-status 403))

            ;; retrieve by another authorized user fails for now
            (-> session-user-view
                (request abs-uri)
                (ltu/body->edn)
                (ltu/is-status 403))

            ;; update the ACL to allow another user to view the external object
            (let [{:keys [acl] :as current-eo} (-> session
                                                   (request abs-uri)
                                                   (ltu/body->edn)
                                                   (ltu/is-operation-present "upload")
                                                   (ltu/is-operation-present "delete")
                                                   (ltu/is-operation-present "edit")
                                                   (ltu/is-operation-absent "ready")
                                                   (ltu/is-operation-absent "download")
                                                   (ltu/is-status 200)
                                                   :response
                                                   :body)
                  view-rule {:principal username-view
                             :type      "USER"
                             :right     "VIEW"}

                  updated-rules (vec (conj (:rules acl) view-rule))

                  updated-eo (-> current-eo
                                 (assoc-in [:acl :rules] updated-rules)
                                 (assoc :name "NEW_VALUE_OK"
                                        :state "BAD_VALUE_IGNORED"))]

              (-> session
                  (request abs-uri
                           :request-method :put
                           :body (json/write-str updated-eo))
                  (ltu/body->edn)
                  (ltu/is-status 200)))

            ;; retrieve by another authorized user MUST NOW SUCCEED
            ;; verify also that name can be updated, but not state
            (let [updated (-> session-user-view
                              (request abs-uri)
                              (ltu/body->edn)
                              (ltu/is-status 200)
                              :response
                              :body)]

              (is (= "NEW_VALUE_OK" (:name updated)))
              (is (not= "BAD_VALUE_IGNORED" (:state updated))))

            ;; anonymous query fails
            (-> session-anon
                (request base-uri)
                (ltu/body->edn)
                (ltu/is-status 403))

            ;; owner query succeeds
            (let [entry (-> session
                            (request base-uri)
                            (ltu/body->edn)
                            (ltu/is-status 200)
                            (ltu/is-resource-uri eo/collection-uri)
                            (ltu/is-count 1)
                            (ltu/entries eo/resource-tag)
                            first)
                  id (:id entry)
                  abs-uri (str p/service-context id)]

              (is (= id uri))

              (let [upload-op (-> session
                                  (request abs-uri)
                                  (ltu/body->edn)
                                  (ltu/is-operation-present "upload")
                                  (ltu/is-operation-present "delete")
                                  (ltu/is-operation-present "edit")
                                  (ltu/is-operation-absent "ready")
                                  (ltu/is-operation-absent "download")
                                  (ltu/is-status 200)
                                  (ltu/get-op "upload"))

                    abs-upload-uri (str p/service-context (u/de-camelcase upload-op))]

                ;; triggering the upload url with anonymous, authorized or unauthorized viewer should fail
                (-> session-anon
                    (request abs-upload-uri
                             :request-method :post)
                    (ltu/body->edn)
                    (ltu/is-status 403))

                (-> session-user-no-view
                    (request abs-upload-uri
                             :request-method :post)
                    (ltu/body->edn)
                    (ltu/is-status 403))

                (-> session-user-view
                    (request abs-upload-uri
                             :request-method :post)
                    (ltu/body->edn)
                    (ltu/is-status 403))

                ;; owner can trigger the upload action
                (-> session
                    (request abs-upload-uri
                             :request-method :post)
                    (ltu/body->edn)
                    (ltu/is-status 200))

                ;; after getting upload URL the state is set to 'uploading'
                ;; 'ready', 'upload', and 'delete' actions are present
                (-> session
                    (request abs-uri)
                    (ltu/body->edn)
                    (ltu/is-status 200)
                    (ltu/is-key-value :state eo/state-uploading)
                    (ltu/is-operation-present "ready")
                    (ltu/is-operation-present "delete")
                    (ltu/is-operation-present "edit")
                    (ltu/is-operation-present "upload")
                    (ltu/is-operation-absent "download"))

                ;; user with view access should see change of state
                ;; actions should be the same
                #_(-> session-user-view
                      (request abs-uri)
                      (ltu/body->edn)
                      (ltu/is-status 200)
                      (ltu/is-key-value :state eo/state-uploading)
                      (ltu/is-operation-absent "ready")
                      (ltu/is-operation-absent "delete")
                      (ltu/is-operation-absent "edit")
                      (ltu/is-operation-absent "upload")
                      (ltu/is-operation-absent "download"))

                ;; doing it again should succeed, a new upload URL can be obtained
                ;; in 'uploading' state
                (-> session
                    (request abs-upload-uri
                             :request-method :post)
                    (ltu/body->edn)
                    (ltu/is-status 200))

                (let [uploading-eo (-> session
                                       (request abs-uri)
                                       (ltu/body->edn)
                                       (ltu/is-operation-present "ready")
                                       (ltu/is-status 200))

                      ready-url-action (str p/service-context (ltu/get-op uploading-eo "ready"))]


                  ;; triggering the ready url with anonymous, authorized or unauthorized viewer should fail
                  (-> session-anon
                      (request ready-url-action
                               :request-method :post)
                      (ltu/body->edn)
                      (ltu/is-status 403))

                  (-> session-user-no-view
                      (request ready-url-action
                               :request-method :post)
                      (ltu/body->edn)
                      (ltu/is-status 403))

                  (-> session-user-view
                      (request ready-url-action
                               :request-method :post)
                      (ltu/body->edn)
                      (ltu/is-status 403))

                  ;; owner can trigger the ready action to prevent further changes to object
                  (-> session
                      (request ready-url-action
                               :request-method :post)
                      (ltu/body->edn)
                      (ltu/is-status 200))

                  (let [ready-eo (-> session
                                     (request abs-uri)
                                     (ltu/body->edn)
                                     (ltu/is-key-value :state eo/state-ready)
                                     (ltu/is-operation-present "download")
                                     (ltu/is-operation-present "delete")
                                     (ltu/is-operation-present "edit")
                                     (ltu/is-operation-absent "upload")
                                     (ltu/is-operation-absent "ready")
                                     (ltu/is-status 200))
                        download-url-action (str p/service-context (ltu/get-op ready-eo "download"))]

                    ;; check states for user with view access
                    #_(-> session-user-view
                          (request abs-uri)
                          (ltu/body->edn)
                          (ltu/is-key-value :state eo/state-ready)
                          (ltu/is-operation-present "download")
                          (ltu/is-operation-absent "delete")
                          (ltu/is-operation-absent "edit")
                          (ltu/is-operation-absent "upload")
                          (ltu/is-operation-absent "ready")
                          (ltu/is-status 200))

                    ;; triggering the download url with anonymous or unauthorized user should fail
                    (-> session-anon
                        (request ready-url-action
                                 :request-method :post)
                        (ltu/body->edn)
                        (ltu/is-status 403))

                    (-> session-user-no-view
                        (request ready-url-action
                                 :request-method :post)
                        (ltu/body->edn)
                        (ltu/is-status 403))

                    ;; triggering download url with owner or user with view access succeeds
                    #_(-> session-user-view
                          (request ready-url-action
                                   :request-method :post)
                          (ltu/body->edn)
                          (ltu/is-status 200))

                    (-> session
                        (request download-url-action
                                 :request-method :post)
                        (ltu/body->edn)
                        (ltu/is-status 200)))))

              ;; owner delete succeeds
              (-> session
                  (request abs-uri
                           :request-method :delete
                           :body (json/write-str {:keep-s3-object true})) ;;no s3 deletion while testing
                  (ltu/body->edn)
                  (ltu/is-status 200))

              ;; ensure entry is really gone
              (-> session
                  (request abs-uri)
                  (ltu/body->edn)
                  (ltu/is-status 404)))))))))
