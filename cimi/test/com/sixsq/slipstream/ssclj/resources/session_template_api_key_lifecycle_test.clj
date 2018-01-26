(ns com.sixsq.slipstream.ssclj.resources.session-template-api-key-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.session-template-api-key :as api-key]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.session-template-lifecycle-test-utils :as stu]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each ltu/with-test-es-client-fixture)
(use-fixtures :once ltu/setup-embedded-zk)

(def base-uri (str p/service-context (u/de-camelcase st/resource-name)))


(def valid-template {:method      api-key/authn-method
                     :instance    api-key/authn-method
                     :name        "API Key"
                     :description "Authentication with API Key and Secret"
                     :key         "key"
                     :secret      "secret"
                     :acl         st/resource-acl})

(deftest lifecycle
  (stu/session-template-lifecycle base-uri (ltu/ring-app) valid-template))

(deftest bad-methods
  (stu/bad-methods base-uri (ltu/ring-app)))
