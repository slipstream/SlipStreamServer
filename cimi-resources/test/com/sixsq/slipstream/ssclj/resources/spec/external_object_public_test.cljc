(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-public-test
  (:require
    [clojure.test :refer [deftest]]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-public :as tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-public :as eo-public]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "USER"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root (merge tpl/resource
                    {:id              "external-object/my-public-object"
                     :resourceURI     eot/resource-uri
                     :created         timestamp
                     :updated         timestamp
                     :acl             valid-acl
                     :state           eo/state-new
                     :publicUrl       "http://bucket.s3.com"})]

    (stu/is-valid ::eo-public/external-object root)

    ;; mandatory keywords
    (doseq [k #{:id :resourceURI :created :updated :acl
                :objectType :state :objectName :bucketName :objectStoreCred}]
      (stu/is-invalid ::eo-public/external-object (dissoc root k)))

    ;; optional keywords
    (doseq [k #{ :publicUrl} ]
      (stu/is-valid ::eo-public/external-object (dissoc root k)))))
