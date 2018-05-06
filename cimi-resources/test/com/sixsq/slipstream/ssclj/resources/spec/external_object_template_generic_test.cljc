(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template-generic-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-generic :as tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template-generic]
    [com.sixsq.slipstream.ssclj.resources.spec.util :as u]))


(deftest test-schema-check
  (let [root (merge tpl/resource
                    {:href "external-object-template/generic"})]

    (u/spec-valid? :cimi.external-object-template.generic/externalObjectTemplate root)

    ;; mandatory keywords
    (doseq [k #{:objectType :objectStoreCred :bucketName :objectName}]
      (u/spec-not-valid? :cimi.external-object-template.generic/externalObjectTemplate (dissoc root k)))

    ;; optional keywords
    (doseq [k #{:contentType :href}]
      (u/spec-valid? :cimi.external-object-template.generic/externalObjectTemplate (dissoc root k)))


    (let [create {:resourceURI            (str eot/resource-uri "Create")
                  :externalObjectTemplate (dissoc root :id)}]
      (u/spec-valid? :cimi/external-object-template.generic-create create))))
