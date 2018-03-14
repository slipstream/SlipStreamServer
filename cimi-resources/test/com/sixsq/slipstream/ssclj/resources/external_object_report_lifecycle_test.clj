(ns com.sixsq.slipstream.ssclj.resources.external-object-report-lifecycle-test
  (:require
    [clojure.test :refer [deftest is are use-fixtures]]
    [peridot.core :refer [session header request content-type]]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as report]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as eo-utils]
    [com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test-utils :as eoltu]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-report :as eor]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as s3]))


(with-redefs [s3/object-store-config (constantly "foo")]
  @eor/reports-bucket)

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase eo/resource-name)))

(def fake-deployment-info {:runUUID     "xxxx-deployment-uuid"
                           :component   "machine.1"
                           :contentType "application/gzip"})

(deftest lifecycle
  (with-redefs [eo-utils/generate-url (constantly "https://s3.example.org/bucket")]
    (eoltu/lifecycle (str p/service-context eot/resource-url "/" report/objectType)
                     fake-deployment-info)))


(deftest check-upload-and-download-operations
  (with-redefs [eo-utils/generate-url (constantly "https://s3.example.org/bucket")]
    (eoltu/upload-and-download-operations (str p/service-context eot/resource-url "/" report/objectType)
                                          fake-deployment-info)))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id eo/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))
