(ns com.sixsq.slipstream.ssclj.resources.module-image-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [are deftest is use-fixtures]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.module-image :as module-image]
    [peridot.core :refer :all]))


(use-fixtures :each ltu/with-test-server-fixture)


(def base-uri (str p/service-context (u/de-camelcase module-image/resource-name)))


(def valid-acl {:owner {:type      "ROLE"
                        :principal "ADMIN"}
                :rules [{:principal "ADMIN"
                         :right     "ALL"
                         :type      "ROLE"}]})


(def timestamp "1964-08-25T10:00:00.0Z")


(def valid-entry {:id           (str module-image/resource-url "/connector-uuid")
                  :resourceURI  module-image/resource-uri
                  :created      timestamp
                  :updated      timestamp
                  :acl          valid-acl

                  :os           "Ubuntu"
                  :loginUser    "ubuntu"
                  :sudo         true

                  :cpu          2
                  :ram          2048
                  :disk         100
                  :volatileDisk 500
                  :networkType  "public"

                  :imageIDs     {:some-cloud       "my-great-image-1"
                                 :some-other-cloud "great-stuff"}

                  :relatedImage {:href "module/other"}})


(deftest lifecycle

  (let [session-anon (-> (session (ltu/ring-app))
                         (content-type "application/json"))
        session-admin (header session-anon authn-info-header "super ADMIN USER ANON")
        session-user (header session-anon authn-info-header "jane USER ANON")]

    ;; create: NOK for anon, users
    (doseq [session [session-anon session-user]]
      (-> session
          (request base-uri
                   :request-method :post
                   :body (json/write-str valid-entry))
          (ltu/body->edn)
          (ltu/is-status 403)))

    ;; queries: OK for admin, NOK for others
    (doseq [session [session-anon session-user]]
      (-> session
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 403)))

    (-> session-admin
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count 0))



    ;; adding, retrieving and  deleting entry as user should succeed
    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-entry))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))

          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; retrieve: OK for admin; NOK for others
      (doseq [session [session-anon session-user]]
        (-> session
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 403)))

      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; delete: OK for admin; NOK for others
      (doseq [session [session-anon session-user]]
        (-> session
            (request abs-uri
                     :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 403)))

      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; verify that the resource was deleted.
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id module-image/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))
