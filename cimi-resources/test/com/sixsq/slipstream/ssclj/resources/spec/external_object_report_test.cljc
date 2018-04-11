(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-report-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.spec.util :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-report]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as tpl]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "USER"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root (merge (dissoc tpl/resource :filename)
                    {:id              "external-object/my-report"
                     :resourceURI     eot/resource-uri
                     :created         timestamp
                     :updated         timestamp
                     :acl             valid-acl
                     :state           eo/state-new
                     :objectName      "object/name/text.txt"
                     :bucketName      "bucket-name"
                     :objectStoreCred {:href "credential/uuid"}})]

    (u/is-valid :cimi/external-object.report root)

    ;; mandatory keywords
    (doseq [k #{:id :resourceURI :created :updated :acl
                :objectType :state :runUUID :component :objectName :bucketName :objectStoreCred}]
      (u/is-not-valid :cimi/external-object.report (dissoc root k)))

    ;; optional keywords
    (doseq [k #{:contentType}]
      (u/is-valid :cimi/external-object.report (dissoc root k)))))
