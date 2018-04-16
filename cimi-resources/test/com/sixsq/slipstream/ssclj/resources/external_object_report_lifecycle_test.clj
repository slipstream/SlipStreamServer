(ns com.sixsq.slipstream.ssclj.resources.external-object-report-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [deftest use-fixtures join-fixtures]]
    [peridot.core :refer [request]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-lifecycle-test-utils :as eoltu]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as report]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]))


(defn update-server-conf-fixture!
  [f]
  (-> eoltu/session-admin
      (request (str p/service-context "configuration/slipstream")
               :request-method :put
               :body (json/write-str {:reportsObjectStoreBucketName "test-bucket"
                                      :reportsObjectStoreCreds      eoltu/*cred-uri*}))
      (ltu/body->edn)
      (ltu/is-status 200))
  (f))

(use-fixtures :each (join-fixtures [ltu/with-test-server-fixture
                                    eoltu/create-connector-fixture!
                                    eoltu/create-cloud-cred-fixture-other-user!
                                    update-server-conf-fixture!
                                    eoltu/s3-redefs!]))

(def base-uri (str p/service-context (u/de-camelcase eo/resource-name)))

(def report-obj-1 {:runUUID     "xxxx-deployment-uuid"
                   :component   "machine.1"
                   :filename    "machine.1_report_time.tgz"
                   :contentType "application/gzip"})

(def report-obj-2 {:runUUID     "xxxx-deployment-uuid"
                   :component   "machine.2"
                   :filename    "machine.2_report_time.tgz"
                   :contentType "application/gzip"})

(def template-url (str p/service-context eot/resource-url "/" report/objectType))

(deftest lifecycle
  (eoltu/lifecycle template-url report-obj-1 report-obj-2))


(deftest check-upload-and-download-operations
  (eoltu/upload-and-download-operations template-url report-obj-1 report-obj-2))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id eo/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))
