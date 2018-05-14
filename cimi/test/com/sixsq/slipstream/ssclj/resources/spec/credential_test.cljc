(ns com.sixsq.slipstream.ssclj.resources.spec.credential-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as cs]
            [com.sixsq.slipstream.ssclj.util.spec :as su]
            [clojure.spec.alpha :as s]
            [com.sixsq.slipstream.ssclj.resources.credential :refer :all]
            ))


(s/def :cimi.test/credential (su/only-keys-maps cs/resource-keys-spec))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest test-configuration-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cred {:id          (str resource-url "/slipstream")
              :resourceURI resource-uri
              :created     timestamp
              :updated     timestamp
              :acl         valid-acl
              :type        "type"
              :method      "method"
              :enabled true
              }]
    (is (s/valid? :cimi.test/credential cred))
    (doseq [k (into #{} (keys (dissoc cred :enabled)))]
      (is (not (s/valid? :cimi.test/credential (dissoc cred k)))))))