(ns com.sixsq.slipstream.ssclj.resources.license-template-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.license-template :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(let [timestamp "1964-08-25T10:00:00.0Z"
      root      {:id          resource-name
                 :resourceURI p/service-context
                 :created     timestamp
                 :updated     timestamp
                 :acl         valid-acl
                 :licenseData "BASE64_LICENSE_DATA"}]

  (expect nil? (s/check LicenseTemplate root))
  (expect (s/check LicenseTemplate (dissoc root :created)))
  (expect (s/check LicenseTemplate (dissoc root :updated)))
  (expect (s/check LicenseTemplate (dissoc root :licenseData)))
  (expect (s/check LicenseTemplate (dissoc root :acl))))


(run-tests [*ns*])

