(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy-test
    (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.alpha :as s]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.credential :as p]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.credential-template-cloud-dummy :refer [credential-type method resource-acl]]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy :as dummy-tpl]))

(def valid-acl resource-acl)

(deftest test-credential-template-cloud-dummy-create-schema-check
  (let [root {:resourceURI        p/resource-uri
              :credentialTemplate {:key       "foo"
                                   :secret    "bar"
                                   :connector {:href "connector/baz"}
                                   :quota     1}}]
    (is (s/valid? ::dummy-tpl/credential-template-create root))
    (doseq [k (into #{} (keys (dissoc root :resourceURI)))]
      (is (not (s/valid? ::dummy-tpl/credential-template-create (dissoc root k)))))))

(deftest test-credential-template-cloud-dummy-schema-check
  (let [timestamp "1972-10-08T10:00:00.0Z"
        root      {:id          (str ct/resource-url "/uuid")
                   :resourceURI p/resource-uri
                   :created     timestamp
                   :updated     timestamp
                   :acl         valid-acl
                   :type        credential-type
                   :method      method
                   :key         "foo"
                   :secret      "bar"
                   :connector   {:href "connector/baz"}
                   :quota       1}]
    (is (s/valid? ::dummy-tpl/credential-template root))
    (doseq [k (into #{} (keys root))]
      (is (not (s/valid? ::dummy-tpl/credential-template (dissoc root k)))))))
