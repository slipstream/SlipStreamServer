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
  {:acl         valid-acl
   :id          sn/resource-name
   :prefix      "schema-org"
   :uri         "https://schema.org/schema1"
   :updated     timestamp
   :created     timestamp
   :resourceURI p/service-context})

(deftest check-prefix
  (are [prefix] (not (nil? (sc/check sn/Prefix prefix)))
                ""
                " prefix "
                "not%allowed"
                "not.allowed"
                "not/allowed"
                "BAD"
                "-bad"
                "bad-"
                "0bad")
  (are [prefix] (nil? (sc/check sn/Prefix prefix))
                "a"
                "a1"
                "alpha"
                "alpha-beta"
                "alpha1"))

(deftest check-service-namespace
  (is (nil? (sc/check sn/ServiceNamespace valid-namespace)))
  (are [namespace] (not (nil? (sc/check sn/ServiceNamespace namespace)))
                   (assoc valid-namespace :uri {:href ""})
                   (dissoc valid-namespace :uri)
                   (assoc valid-namespace :uri {})
                   (assoc valid-namespace :uri "")
                   (dissoc valid-namespace :prefix)
                   (assoc valid-namespace :prefix "")))
