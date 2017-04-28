(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.configuration :refer :all]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template :as cts]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def :cimi.test/configuration (su/only-keys-maps cts/resource-keys-spec))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest test-configuration-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cfg {:id          (str resource-url "/slipstream")
             :resourceURI resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl
             :service     "foo"}]
    (is (s/valid? :cimi.test/configuration cfg))
    (doseq [k (into #{} (keys (dissoc cfg :id :resourceURI)))]
      (is (not (s/valid? :cimi.test/configuration (dissoc cfg k)))))))
