(ns com.sixsq.slipstream.ssclj.resources.user-params-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.user-params :as up]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each ltu/with-test-es-client-fixture)
(use-fixtures :once ltu/setup-embedded-zk)

(def base-uri (str p/service-context (u/de-camelcase up/resource-name)))

#_(dyn/initialize)

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id up/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :put]
                            [resource-uri :post]])))
