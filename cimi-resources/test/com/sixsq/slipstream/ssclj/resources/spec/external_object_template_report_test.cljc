(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report-test
  (:require
    [clojure.test :refer [deftest is]]
    [expound.alpha :as expound]

    [com.sixsq.slipstream.ssclj.resources.spec.external-object-report :as rs]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


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
              :objectType  "report"
              :state       "new"
              :contentType "text/html; charset=utf-8"
              :filename    "text.txt"
              }]

    (expound/expound :cimi.external-object-template.report/externalObjectTemplate root)
    (is (s/valid? :cimi.external-object-template.report/externalObjectTemplate root))))
