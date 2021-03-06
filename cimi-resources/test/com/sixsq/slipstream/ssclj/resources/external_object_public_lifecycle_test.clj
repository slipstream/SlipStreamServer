(ns com.sixsq.slipstream.ssclj.resources.external-object-public-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [deftest join-fixtures use-fixtures]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test-utils :as eoltu]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-public :as public]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as s3]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [peridot.core :refer :all])
  (:import (com.amazonaws AmazonServiceException)))


(use-fixtures :each (join-fixtures [ltu/with-test-server-fixture
                                    eoltu/create-connector-fixture!
                                    eoltu/create-cloud-cred-fixture!
                                    eoltu/s3-redefs!]))


(def base-uri (str p/service-context (u/de-camelcase eo/resource-name)))


(defn external-object
  []
  {:bucketName      "my-bucket"
   :objectStoreCred {:href eoltu/*cred-uri*}
   :contentType     "application/gzip"
   :objectName      "my/obj/name-1"})


(deftest lifecycle
  (eoltu/full-eo-lifecycle (str p/service-context eot/resource-url "/" public/objectType)
                           (external-object)))


(defn throw-any-aws-exception [_ _ _]
  (let [ex (doto
             (AmazonServiceException. "Simulated AWS Exception")
             (.setStatusCode 400))]
    (throw ex)))


(deftest check-public-access
  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-user (header session-anon authn-info-header eoltu/user-info-header)
        base-uri (str p/service-context (u/de-camelcase eo/resource-name))
        template (eoltu/get-template (str p/service-context eot/resource-url "/" public/objectType))
        create-href {:externalObjectTemplate (-> (external-object)
                                                 (assoc :href (:id template))
                                                 (dissoc :objectType))}]

    ;; Create the test object.
    (-> session-user
        (request base-uri
                 :request-method :post
                 :body (json/write-str create-href))
        (ltu/body->edn)
        (ltu/is-status 201)
        (ltu/location))

    (let [entry (-> session-user
                    (request base-uri)
                    (ltu/body->edn)
                    (ltu/is-status 200)
                    (ltu/is-resource-uri eo/collection-uri)
                    (ltu/is-count 1)
                    (ltu/entries eo/resource-tag)
                    first)
          id (:id entry)
          abs-uri (str p/service-context id)
          upload-op (-> session-user
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

      ;; Upload object's content.
      (-> session-user
          (request abs-upload-uri
                   :request-method :post)
          (ltu/body->edn)
          (ltu/is-status 200))

      (let [uploading-eo (-> session-user
                             (request abs-uri)
                             (ltu/body->edn)
                             (ltu/is-operation-present "ready")
                             (ltu/is-status 200))

            ready-url-action (str p/service-context (ltu/get-op uploading-eo "ready"))]


        ;; Missing ACL should fail the action
        (with-redefs [s3/set-acl-public-read throw-any-aws-exception]
          (-> session-user
              (request ready-url-action
                       :request-method :post)
              (ltu/body->edn)
              (ltu/is-status 500)))


        ;; With public ACL the public URL should be set on ready action
        (with-redefs [s3/set-acl-public-read (fn [_ _ _] nil)
                      s3/s3-url (fn [_ _ _] "https://my-object.s3.com")]
          (-> session-user
              (request ready-url-action
                       :request-method :post)
              (ltu/body->edn)
              (ltu/is-key-value :URL "https://my-object.s3.com")
              (ltu/is-status 200)))))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id eo/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))


