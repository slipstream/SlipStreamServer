(ns com.sixsq.slipstream.ssclj.resources.configuration-nuvlabox-identifier-lifecycle-test
  (:require
    [clojure.test :refer [deftest use-fixtures]]
    [com.sixsq.slipstream.ssclj.resources.configuration-lifecycle-test-utils :as test-utils]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-nuvlabox-identifier :as nbid]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-session-oidc :as oidc]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as slipstream]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]))

(use-fixtures :each ltu/with-test-server-fixture)


(deftest lifecycle-nuvlabox-identifier
  (test-utils/check-lifecycle nbid/service :identifiers [{:name "name1"}] "NEW_ID"))
