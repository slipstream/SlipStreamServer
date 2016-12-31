(ns com.sixsq.slipstream.ssclj.resources.service-attribute-namespace-schema-test
  (:require
    [clojure.test :refer :all]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :as sn]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [schema.core :as sc]))

(def timestamp "1970-04-16T08:40:00.0Z")

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})
(def valid-namespace
  {:acl       valid-acl
   :id        sn/resource-name
   :prefix    "schema-org"
   :uri       "https://schema.org/schema1"
   :updated   timestamp
   :created   timestamp
   :resourceURI p/service-context})

(deftest test-service-namespace-schema
  (is (nil? (sc/check sn/ServiceNamespace valid-namespace)))
  (is (sc/check sn/ServiceNamespace (dissoc valid-namespace :uri)))
  (is (sc/check sn/ServiceNamespace (assoc valid-namespace :uri {})))
  (is (sc/check sn/ServiceNamespace (assoc valid-namespace :uri {:href ""})))
  (is (sc/check sn/ServiceNamespace (assoc valid-namespace :uri "")))
  (is (sc/check sn/ServiceNamespace (dissoc valid-namespace :prefix)))
  (is (sc/check sn/ServiceNamespace (assoc valid-namespace :prefix ""))))

(deftest prefix-not-nil-and-no-dot-no-slash
  (is (nil? (sc/check sn/ServiceNamespace (assoc valid-namespace :prefix "ab"))))
  (is (sc/check sn/ServiceNamespace (assoc valid-namespace :prefix "")))
  (is (sc/check sn/ServiceNamespace (dissoc valid-namespace :prefix)))
  (is (sc/check sn/ServiceNamespace (assoc valid-namespace :prefix "a.b")))
  (is (sc/check sn/ServiceNamespace (assoc valid-namespace :prefix "a/b"))))
