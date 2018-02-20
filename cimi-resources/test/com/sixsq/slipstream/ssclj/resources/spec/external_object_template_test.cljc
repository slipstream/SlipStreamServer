(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template-test
  (:require [clojure.test :refer [deftest is]]
            [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
            [com.sixsq.slipstream.ssclj.resources.spec.external-object-template :as eots]
            [clojure.spec.alpha :as s]
            [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def :cimi.test/external-object-template (su/only-keys-maps eots/resource-keys-spec))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "USER"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id          (str eot/resource-url "/uuid")
              :resourceURI eot/resource-uri
              :created     timestamp
              :updated     timestamp
              :acl         valid-acl
              :objectType  "alpha"
              :state       "new"}]
    
    (is (s/valid? :cimi.test/external-object-template root))

    ;;mandatory keywords
    (doseq [k #{:objectType :id :resourceURI :state}]
      (is (not (s/valid? :cimi.test/external-object-template (dissoc root k)))))))
