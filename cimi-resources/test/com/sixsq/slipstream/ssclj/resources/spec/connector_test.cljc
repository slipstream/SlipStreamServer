(ns com.sixsq.slipstream.ssclj.resources.spec.connector-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.connector :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.connector :as cs]
    [com.sixsq.slipstream.ssclj.resources.spec.connector-template :as cts]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def :cimi.test/connector (su/only-keys-maps cts/resource-keys-spec))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id                  (str c/resource-url "/connector-uuid")
              :resourceURI         c/resource-uri
              :created             timestamp
              :updated             timestamp
              :acl                 valid-acl
              :cloudServiceType    "alpha"
              :orchestratorImageid "123"
              :quotaVm             "20"
              :maxIaasWorkers      5
              :instanceName        "foo"}]
    (is (s/valid? :cimi.test/connector root))
    (doseq [k (into #{} (keys (dissoc root :id :resourceURI)))]
      (is (not (s/valid? :cimi.test/connector (dissoc root k)))))))
