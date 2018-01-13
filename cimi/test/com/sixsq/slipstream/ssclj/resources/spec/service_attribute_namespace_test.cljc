(ns com.sixsq.slipstream.ssclj.resources.spec.service-attribute-namespace-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :as sn]))

(def timestamp "1970-04-16T08:40:00.0Z")

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})
(def valid-namespace
  {:acl         valid-acl
   :id          (str sn/resource-url "/uuid")
   :prefix      "schema-org"
   :uri         "https://schema.org/schema1"
   :updated     timestamp
   :created     timestamp
   :resourceURI sn/resource-uri})

(deftest check-prefix
  (are [prefix] (not (s/valid? :cimi.service-attribute-namespace/prefix prefix))
                ""
                " prefix "
                "not%allowed"
                "not.allowed"
                "not/allowed"
                "BAD"
                "-bad"
                "bad-"
                "0bad")
  (are [prefix] (s/valid? :cimi.service-attribute-namespace/prefix prefix)
                "a"
                "a1"
                "alpha"
                "alpha-beta"
                "alpha1"))

(deftest check-service-namespace
  (is (s/valid? :cimi/service-attribute-namespace valid-namespace))
  (are [namespace] (not (s/valid? :cimi/service-attribute-namespace namespace))
                   (assoc valid-namespace :uri {:href ""})
                   (dissoc valid-namespace :uri)
                   (assoc valid-namespace :uri {})
                   (assoc valid-namespace :uri "")
                   (dissoc valid-namespace :prefix)
                   (assoc valid-namespace :prefix "")))
