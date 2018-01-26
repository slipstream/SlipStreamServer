(ns com.sixsq.slipstream.ssclj.resources.credential-lifecycle-test
  (:require
    [clojure.test :refer [deftest is are use-fixtures]]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.credential :as credential]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each ltu/with-test-es-client-fixture)
(use-fixtures :once ltu/setup-embedded-zk)

(def base-uri (str p/service-context credential/resource-url))

;; initialize must to called to pull in CredentialTemplate resources
#_(dyn/initialize)

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id credential/resource-url))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))
