(ns com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.external-object :refer :all]
    [com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test-utils :as eoltu]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-generic :as generic]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.connector :as c]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as cont]
    [com.sixsq.slipstream.ssclj.resources.connector-template-alpha-example :as con-alpha]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as credt]
    [com.sixsq.slipstream.ssclj.resources.credential-template-cloud-alpha :as cred-alpha]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.credential :as cred]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as s3]))


(use-fixtures :each (join-fixtures [ltu/with-test-server-fixture
                                    eoltu/create-connector-fixture!
                                    eoltu/create-cloud-cred-fixture!]))

(def base-uri (str p/service-context (u/de-camelcase eo/resource-name)))

(defn external-object
  []
  {:bucketName      "my-bucket"
   :objectStoreCred {:href eoltu/*cred-uri*}
   :contentType     "application/gzip"})

(defn external-object-with-name
  [name]
  (assoc (external-object) :objectName name))

(def base-uri (str p/service-context (u/de-camelcase resource-name)))


(deftest lifecycle
  (eoltu/lifecycle (str p/service-context eot/resource-url "/" generic/objectType)
                   (external-object)))


(deftest check-upload-and-download-operations
  (eoltu/upload-and-download-operations (str p/service-context eot/resource-url "/" generic/objectType)
                                        (external-object)))


(deftest lifecycle-href
  (let [href-create   {:externalObjectTemplate (assoc (external-object-with-name "my-obj-name")
                                                 :state eo/state-new
                                                 :objectType generic/objectType)}]

    ;; abbreviated lifecycle using href to template instead of copy
    (let [uri     (-> eoltu/session-admin
                      (request base-uri
                               :request-method :post
                               :body (json/write-str href-create))
                      (ltu/body->edn)
                      (ltu/is-status 201)
                      (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin delete succeeds
      (with-redefs [s3/delete-s3-object (fn [_ _ _] nil)]
        (-> eoltu/session-admin
           (request abs-uri
                    :request-method :delete)
           (ltu/body->edn)
           (ltu/is-status 200)))

      ;; ensure entry is really gone
      (-> eoltu/session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))


