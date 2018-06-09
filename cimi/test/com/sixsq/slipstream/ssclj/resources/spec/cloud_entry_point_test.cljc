(ns com.sixsq.slipstream.ssclj.resources.spec.cloud-entry-point-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.cloud-entry-point :refer :all]
    [com.sixsq.slipstream.ssclj.resources.spec.cloud-entry-point :as cep]))

(defn valid? [cep] (s/valid? ::cep/cloud-entry-point cep))
(def invalid? (complement valid?))

(deftest check-root-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id          resource-url
              :resourceURI p/service-context
              :created     timestamp
              :updated     timestamp
              :acl         resource-acl
              :baseURI     "http://cloud.example.org/"}]

    (is (valid? root))
    (is (valid? (assoc root :resources {:href "resource/uuid"})))
    (is (invalid? (dissoc root :created)))
    (is (invalid? (dissoc root :updated)))
    (is (invalid? (dissoc root :baseURI)))
    (is (invalid? (dissoc root :acl)))))
