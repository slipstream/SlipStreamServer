(ns com.sixsq.slipstream.ssclj.resources.configuration-nuvlabox-identifier-lifecycle-test
  (:require
    [clojure.test :refer [deftest use-fixtures]]
    [com.sixsq.slipstream.ssclj.resources.configuration-lifecycle-test-utils :as test-utils]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-nuvlabox-identifier :as nbi]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]))

(use-fixtures :each ltu/with-test-server-fixture)


(deftest lifecycle-nuvlabox-identifier
  (test-utils/check-lifecycle nbi/service :identifiers [{:name "id1"}] [{:name "id2"}]))
