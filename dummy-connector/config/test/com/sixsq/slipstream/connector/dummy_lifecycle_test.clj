(ns com.sixsq.slipstream.connector.dummy-lifecycle-test
    (:require
    [clojure.test :refer :all]

    [com.sixsq.slipstream.connector.dummy-template :as cit]
    [com.sixsq.slipstream.ssclj.resources.connector-test-utils :as tu]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]))

(use-fixtures :each ltu/with-test-server-fixture)


(deftest lifecycle
  (tu/connector-lifecycle cit/cloud-service-type))

