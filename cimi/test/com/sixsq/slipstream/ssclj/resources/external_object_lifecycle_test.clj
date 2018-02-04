(ns com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test
  (:require [clojure.test :refer :all]
            [peridot.core :refer :all]
            [com.sixsq.slipstream.ssclj.resources.external-object :refer :all]
            [com.sixsq.slipstream.ssclj.resources.external-object-template-alpha-example :as example]
            [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
            [com.sixsq.slipstream.ssclj.resources.external-object-test-utils :as tu]
            [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
            [com.sixsq.slipstream.ssclj.app.params :as p]
            [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
            [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
            [clojure.data.json :as json]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))


(deftest lifecycle
  (let [href (str eot/resource-url "/" example/objectType)
        template-url (str p/service-context eot/resource-url "/" example/objectType)
        session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-user (header session-anon authn-info-header "jane USER")
        session-admin (header session-anon authn-info-header "root ADMIN")

        resp (-> session-admin
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))
        template (get-in resp [:response :body])
        valid-create {:externalObjectTemplate (ltu/strip-unwanted-attrs (merge template {:alphaKey     2001
                                                                                         :instanceName "alpha-omega"}))}
        href-create {:externalObjectTemplate {:href         href
                                              :alphaKey     3001
                                              :instanceName "alpha-omega"}}
        invalid-create (assoc-in valid-create [:externalObjectTemplate :invalid] "BAD")]


    ;; anonymous create should fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-create))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; user create should also fail
    (-> session-user
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

    ;; full external object lifecycle as administrator should work
    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

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
                        (ltu/is-resource-uri collection-uri)
                        (ltu/is-count #(= 1 %))
                        (ltu/entries resource-tag))]
        (is ((set (map :id entries)) uri))

        ;; verify that all entries are accessible
        (let [pair-fn (juxt :id #(str p/service-context (:id %)))
              pairs (map pair-fn entries)]
          (doseq [[id entry-uri] pairs]
            (-> session-admin
                (request entry-uri)
                (ltu/body->edn)
                (ltu/is-operation-present "delete")
                (ltu/is-operation-present "upload")
                (ltu/is-operation-present "download")
                (ltu/is-operation-absent "edit")
                (ltu/is-status 200)
                (ltu/is-id id)))))

      ;; admin delete succeeds
      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))


    ;; abbreviated lifecycle using href to template instead of copy
    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str href-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin delete succeeds
      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))

(deftest upload-operation
  (let [href (str eot/resource-url "/" example/objectType)
        template-url (str p/service-context eot/resource-url "/" example/objectType)
        session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-user (header session-anon authn-info-header "jane USER")
        session-admin (header session-anon authn-info-header "root ADMIN")

        resp (-> session-admin
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))

        ;; anonymous query is not authorized
        resp-anon (-> session-anon
                      (request template-url)
                      (ltu/body->edn)
                      (ltu/is-status 403))

        ;; user query is not authorized
        resp-user (-> session-user
                      (request template-url)
                      (ltu/body->edn)
                      (ltu/is-status 403))

        template (get-in resp [:response :body])

        valid-create {:externalObjectTemplate (ltu/strip-unwanted-attrs (merge template {:alphaKey     2002
                                                                                         :instanceName "alpha-gamma"}))}

        ]

    (let [uri (-> session-admin
                  (request tu/base-uri
                           :request-method :post
                           :body (json/write-str valid-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))

          ;; anonymous create should fail
          uri-anon (-> session-anon
                       (request tu/base-uri
                                :request-method :post
                                :body (json/write-str valid-create))
                       (ltu/body->edn)
                       (ltu/is-status 403))
          ;; user create should fail
          uri-user (-> session-user
                       (request tu/base-uri
                                :request-method :post
                                :body (json/write-str valid-create))
                       (ltu/body->edn)
                       (ltu/is-status 403))
          abs-uri (str p/service-context (u/de-camelcase uri))

          upload-op (-> session-admin
                        (request abs-uri)
                        (ltu/body->edn)
                        #_(ltu/dump)
                        (ltu/is-operation-present "upload")
                        (ltu/is-status 200)
                        (ltu/get-op "upload"))

          abs-upload-uri (str p/service-context (u/de-camelcase upload-op))

          ;;upload should be possible for ADMIN
          upload-resp (-> session-admin
                          (request abs-upload-uri
                                   :request-method :post
                                   )
                          (ltu/body->edn)
                          (ltu/dump)
                          (ltu/is-status 200)
                          :response
                          :body)

          ]

      (-> session-anon
          (request abs-upload-uri
                   :request-method :post
                   )
          (ltu/body->edn)
          (ltu/dump)
          (ltu/is-status 200)
          :response
          :body)

      (-> session-user
          (request abs-upload-uri
                   :request-method :post
                   )
          (ltu/body->edn)
          (ltu/dump)
          (ltu/is-status 200)
          :response
          :body)


      ;;the reponse of an upload operation contains the uploadUri
      (is (:uploadUri upload-resp))

      ;;the upload operation should only be ran once
      (-> session-admin
          (request abs-upload-uri
                   :request-method :post
                   )
          (ltu/body->edn)
          (ltu/is-status 400)
          :response
          :body)

      )
    )

  )

(deftest download-operation
  (let [href (str eot/resource-url "/" example/objectType)
        template-url (str p/service-context eot/resource-url "/" example/objectType)
        session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-user (header session-anon authn-info-header "jane USER")
        session-admin (header session-anon authn-info-header "root ADMIN")

        resp (-> session-admin
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))

        ;; anonymous query is not authorized
        resp-anon (-> session-anon
                      (request template-url)
                      (ltu/body->edn)
                      (ltu/is-status 403))

        ;; user query is not authorized
        resp-user (-> session-user
                      (request template-url)
                      (ltu/body->edn)
                      (ltu/is-status 403))

        template (get-in resp [:response :body])

        valid-create {:externalObjectTemplate (ltu/strip-unwanted-attrs (merge template {:alphaKey     2002
                                                                                         :instanceName "alpha-gamma"}))}

        ]

    (let [uri (-> session-admin
                  (request tu/base-uri
                           :request-method :post
                           :body (json/write-str valid-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))

          ;; anonymous create should fail
          uri-anon (-> session-anon
                       (request tu/base-uri
                                :request-method :post
                                :body (json/write-str valid-create))
                       (ltu/body->edn)
                       (ltu/is-status 403))
          ;; user create should fail
          uri-user (-> session-user
                       (request tu/base-uri
                                :request-method :post
                                :body (json/write-str valid-create))
                       (ltu/body->edn)
                       (ltu/is-status 403))
          abs-uri (str p/service-context (u/de-camelcase uri))

          download-op (-> session-admin
                        (request abs-uri)
                        (ltu/body->edn)
                        #_(ltu/dump)
                        (ltu/is-operation-present "download")
                        (ltu/is-status 200)
                        (ltu/get-op "download"))

          abs-download-uri (str p/service-context (u/de-camelcase download-op))

          ;;download operation should be possible for ADMIN
          download-resp (-> session-admin
                          (request abs-download-uri
                                   :request-method :post
                                   )
                          (ltu/body->edn)
                          (ltu/dump)
                          (ltu/is-status 200)
                          :response
                          :body)

          ]

      (clojure.pprint/pprint (str "download-op " download-op))

      (-> session-anon
          (request abs-download-uri
                   :request-method :post
                   )
          (ltu/body->edn)
          (ltu/dump)
          (ltu/is-status 200)
          :response
          :body)

      (-> session-user
          (request abs-download-uri
                   :request-method :post
                   )
          (ltu/body->edn)
          (ltu/dump)
          (ltu/is-status 200)
          :response
          :body)


      ;;the download operation can be repeated
      (-> session-admin
          (request abs-download-uri
                   :request-method :post
                   )
          (ltu/body->edn)
          (ltu/dump)
          (ltu/is-status 200)
          :response
          :body)

      )
    )

  )


