(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-report-test
  (:require
    [clojure.test :refer [deftest is]]
    [clojure.spec.alpha :as s]
    [expound.alpha :refer [expound-str]]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-report]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as tpl]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "USER"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(defn is-valid
  [spec resource]
  (is (s/valid? spec resource) (expound-str spec resource)))

(defn is-not-valid
  [spec resource]
  (is (not (s/valid? spec resource)) (expound-str spec resource)))

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

    (is-valid :cimi/external-object.report root)

    ;; mandatory keywords
    (doseq [k #{:id :resourceURI :created :updated :acl
                :objectType :state :runUUID :component :objectName :bucketName :objectStoreCred}]
      (is-not-valid :cimi/external-object.report (dissoc root k)))

    ;; optional keywords
    (doseq [k #{:contentType}]
      (is-valid :cimi/external-object.report (dissoc root k)))))
