(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report-test
  (:require
    [clojure.test :refer [deftest is]]
    [clojure.spec.alpha :as s]
    [expound.alpha :refer [expound-str]]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report]))


(defn is-valid
  [spec resource]
  (is (s/valid? spec resource) (expound-str spec resource)))

(defn is-not-valid
  [spec resource]
  (is (not (s/valid? spec resource)) (expound-str spec resource)))

(deftest test-schema-check
  (let [root (merge tpl/resource
                    {:href     "external-object-template/report"
                     :filename "component.1_report_time.tgz"})]

    (is-valid :cimi.external-object-template.report/externalObjectTemplate root)

    ;; mandatory keywords
    (doseq [k #{:objectType :runUUID :component :filename}]
      (is-not-valid :cimi.external-object-template.report/externalObjectTemplate (dissoc root k)))

    ;; optional keywords
    (doseq [k #{:contentType :href}]
      (is-valid :cimi.external-object-template.report/externalObjectTemplate (dissoc root k)))


    (let [create {:resourceURI            (str eot/resource-uri "Create")
                  :externalObjectTemplate (dissoc root :id)}]
      (is-valid :cimi/external-object-template.report-create create))))
