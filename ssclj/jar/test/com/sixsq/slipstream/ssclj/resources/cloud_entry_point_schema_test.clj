(ns com.sixsq.slipstream.ssclj.resources.cloud-entry-point-schema-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.cloud-entry-point :refer :all]
    [com.sixsq.slipstream.ssclj.resources.cloud-entry-point.spec :as schema]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [clojure.spec :as spec]
    [com.sixsq.slipstream.ssclj.app.params :as p])
  (:import (clojure.lang ExceptionInfo)))

(let [timestamp "1964-08-25T10:00:00.0Z"
      valid-cep {:id          resource-name
                 :resourceURI resource-uri
                 :created     timestamp
                 :updated     timestamp
                 :acl         resource-acl
                 :baseURI     "http://cloud.example.org/"}]

  (is (= valid-cep (crud/validate valid-cep)))

  (let [updated-cep (assoc valid-cep :resources {:href "Resource/uuid"})]
    (is (= updated-cep (crud/validate updated-cep))))

  (is (thrown? ExceptionInfo (crud/validate (dissoc valid-cep :created))))
  (is (thrown? ExceptionInfo (crud/validate (dissoc valid-cep :updated))))
  (is (thrown? ExceptionInfo (crud/validate (dissoc valid-cep :baseURI))))
  (is (thrown? ExceptionInfo (crud/validate (dissoc valid-cep :acl)))))
