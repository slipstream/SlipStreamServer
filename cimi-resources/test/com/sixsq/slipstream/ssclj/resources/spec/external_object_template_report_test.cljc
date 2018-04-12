(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.spec.util :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report]))


(deftest test-schema-check
  (let [root (merge tpl/resource
                    {:href     "external-object-template/report"
                     :filename "component.1_report_time.tgz"})]

    (u/spec-valid? :cimi.external-object-template.report/externalObjectTemplate root)

    ;; mandatory keywords
    (doseq [k #{:objectType :runUUID :component :filename}]
      (u/spec-not-valid? :cimi.external-object-template.report/externalObjectTemplate (dissoc root k)))

    ;; optional keywords
    (doseq [k #{:contentType :href}]
      (u/spec-valid? :cimi.external-object-template.report/externalObjectTemplate (dissoc root k)))


    (let [create {:resourceURI            (str eot/resource-uri "Create")
                  :externalObjectTemplate (dissoc root :id)}]
      (u/spec-valid? :cimi/external-object-template.report-create create))))
