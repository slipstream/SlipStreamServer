(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-generic-test
  (:require
    [clojure.test :refer [deftest is]]
    [clojure.spec.alpha :as s]
    [expound.alpha :refer [expound-str]]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-generic]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-generic :as tpl]
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
        root (merge tpl/resource
                    {:id              "external-object/my-report"
                     :resourceURI     eot/resource-uri
                     :created         timestamp
                     :updated         timestamp
                     :acl             valid-acl
                     :state           eo/state-new})]

    (is-valid :cimi/external-object.generic root)

    ;; mandatory keywords
    (doseq [k #{:id :resourceURI :created :updated :acl
                :objectType :state :objectName :bucketName :objectStoreCred}]
      (is-not-valid :cimi/external-object.generic (dissoc root k)))

    ;; optional keywords
    (doseq [k #{:contentType}]
      (is-valid :cimi/external-object.generic (dissoc root k)))))
