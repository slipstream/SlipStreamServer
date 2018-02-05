(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [com.sixsq.slipstream.ssclj.resources.external-object :as o]
            [com.sixsq.slipstream.ssclj.resources.spec.external-object :as eo]
            [com.sixsq.slipstream.ssclj.resources.spec.external-object-template :as eot]
            [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def :cimi.test/externalObject (su/only-keys-maps eot/resource-keys-spec))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "USER"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id                  (str o/resource-url "/uuid")
              :resourceURI         o/resource-uri
              :created             timestamp
              :updated             timestamp
              :acl                 valid-acl
              :instanceName           "foo"
              :objectType               "report"
              :state "new"}]


    (is (s/valid? :cimi.test/externalObject root))

    ;;mandatory keywords
    (doseq [k #{:objectType :instanceName :id :resourceURI :state}]
      (is (not (s/valid? :cimi.test/externalObject (dissoc root k)))))

    ;; optional keywords
    (doseq [k #{ :uri}]
      (is (s/valid? :cimi.test/externalObject (dissoc root k))))))

