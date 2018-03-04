(ns com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test-utils
  (:require
    [clojure.test :refer [is]]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.external-object :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]))


(def base-uri (str p/service-context (u/de-camelcase eo/resource-name)))


(defn lifecycle
  [template-url template-data]
  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-user (header session-anon authn-info-header "jane USER")
        session-admin (header session-anon authn-info-header "root ADMIN")

        resp (-> session-admin
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))
        template (get-in resp [:response :body])

        valid-create {:externalObjectTemplate (merge (ltu/strip-unwanted-attrs template) template-data)}

        invalid-create (assoc-in valid-create [:externalObjectTemplate :invalid] "BAD")]

    ;; anonymous create should fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-create))
        (ltu/body->edn)
        (ltu/is-status 403))

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
        session-user (header session-anon authn-info-header "jane USER")
        session-admin (header session-anon authn-info-header "root ADMIN")

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

    ;; anonymous create should fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-create))
        (ltu/body->edn)
        (ltu/is-status 403))

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
