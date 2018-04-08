(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template-generic-test
  (:require
    [clojure.test :refer [deftest is]]
    [clojure.spec.alpha :as s]
    [expound.alpha :refer [expound-str]]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-generic :as tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template-generic]))


(defn is-valid
  [spec resource]
  (is (s/valid? spec resource) (expound-str spec resource)))

(defn is-not-valid
  [spec resource]
  (is (not (s/valid? spec resource)) (expound-str spec resource)))

(deftest test-schema-check
  (let [root (merge tpl/resource
                    {:href "external-object-template/generic"})]

    (is-valid :cimi.external-object-template.generic/externalObjectTemplate root)

    ;; mandatory keywords
    (doseq [k #{:objectType :objectStoreCred :bucketName :objectName}]
      (is-not-valid :cimi.external-object-template.generic/externalObjectTemplate (dissoc root k)))

    ;; optional keywords
    (doseq [k #{:contentType :href}]
      (is-valid :cimi.external-object-template.generic/externalObjectTemplate (dissoc root k)))


    (let [create {:resourceURI            (str eot/resource-uri "Create")
                  :externalObjectTemplate (dissoc root :id)}]
      (is-valid :cimi/external-object-template.generic-create create))))
