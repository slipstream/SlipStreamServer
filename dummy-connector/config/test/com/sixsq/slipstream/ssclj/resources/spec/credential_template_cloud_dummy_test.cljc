(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy-test
  (:require
    [clojure.test :refer [deftest]]
    [com.sixsq.slipstream.ssclj.resources.credential :as p]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.credential-template-cloud-dummy :refer [credential-type method resource-acl]]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy :as dummy-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid-acl resource-acl)


(deftest test-credential-template-cloud-dummy-create-schema-check
  (let [root {:resourceURI        p/resource-uri
              :credentialTemplate {:key                "foo"
                                   :secret             "bar"
                                   :connector          {:href "connector/baz"}
                                   :quota              1
                                   :disabledMonitoring false}}]
    (stu/is-valid ::dummy-tpl/credential-template-create root)
    (doseq [k (keys (dissoc root :resourceURI))]
      (stu/is-invalid ::dummy-tpl/credential-template-create (dissoc root k)))))


(deftest test-credential-template-cloud-dummy-schema-check
  (let [timestamp "1972-10-08T10:00:00.0Z"
        root {:id                 (str ct/resource-url "/uuid")
              :resourceURI        p/resource-uri
              :created            timestamp
              :updated            timestamp
              :acl                valid-acl
              :type               credential-type
              :method             method
              :key                "foo"
              :secret             "bar"
              :connector          {:href "connector/baz"}
              :quota              1
              :disabledMonitoring false}]
    (stu/is-valid ::dummy-tpl/credential-template root)
    (doseq [k (keys (dissoc root :disabledMonitoring))]
      (stu/is-invalid ::dummy-tpl/credential-template (dissoc root k)))
    (doseq [k [:disabledMonitoring]]
      (stu/is-valid ::dummy-tpl/credential-template (dissoc root k)))))
