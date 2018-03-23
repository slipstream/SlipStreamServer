(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report-test
  (:require
    [clojure.test :refer [deftest is]]
    [clojure.spec.alpha :as s]

    [com.sixsq.slipstream.ssclj.resources.spec.external-object-report]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report]

    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "USER"
                         :type      "ROLE"
                         :right     "VIEW"}]})


(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id          "external-object/my-report"
              :resourceURI eot/resource-uri
              :created     timestamp
              :updated     timestamp
              :acl         valid-acl
              :objectType  "report"
              :state       "new"
              :contentType "text/html; charset=utf-8"
              :filename    "text.txt"
              :runUUID     "xyz"
              :component   "machine.1"}]

    ;;
    ;; schemas for ExternalObjectReport and ExternalObjectTemplateReport are the same
    ;; test both at the same time
    ;;

    #_(expound/expound :cimi.external-object-template.report/externalObjectTemplate root)
    (is (s/valid? :cimi/external-object.report root))
    (is (s/valid? :cimi/external-object-template.report root))

    ;; mandatory keywords
    (doseq [k #{:id :resourceURI :created :updated :acl :objectType :state :runUUID :component}]
      (is (not (s/valid? :cimi/external-object.report (dissoc root k))))
      (is (not (s/valid? :cimi/external-object-template.report (dissoc root k)))))

    ;; optional keywords
    (doseq [k #{:contentType :filename}]
      (is (s/valid? :cimi/external-object.report (dissoc root k)))
      (is (s/valid? :cimi/external-object-template.report (dissoc root k))))

    ;;
    ;; verify the create schema for the report external object
    ;;

    (let [create {:resourceURI            (str eot/resource-uri "Create")
                  :externalObjectTemplate (dissoc root :id)}]
      (is (s/valid? :cimi/external-object-template.report-create create)))))
