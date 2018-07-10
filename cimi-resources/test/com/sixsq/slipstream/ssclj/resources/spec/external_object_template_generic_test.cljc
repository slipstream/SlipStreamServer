(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template-generic-test
  (:require
    [clojure.test :refer [deftest]]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-generic :as tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template-generic :as eot-generic]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(deftest test-schema-check
  (let [root (merge tpl/resource
                    {:href "external-object-template/generic"})]

    (stu/is-valid ::eot-generic/externalObjectTemplate root)

    ;; mandatory keywords
    (doseq [k #{:objectType :objectStoreCred :bucketName :objectName}]
      (stu/is-invalid ::eot-generic/externalObjectTemplate (dissoc root k)))

    ;; optional keywords
    (doseq [k #{:contentType :href}]
      (stu/is-valid ::eot-generic/externalObjectTemplate (dissoc root k)))


    (let [create {:resourceURI            (str eot/resource-uri "Create")
                  :externalObjectTemplate (dissoc root :id)}]
      (stu/is-valid ::eot-generic/external-object-create create))))
