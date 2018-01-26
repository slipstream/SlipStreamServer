(ns com.sixsq.slipstream.ssclj.resources.configuration-lifecycle-test
  (:require
    [clojure.test :refer [deftest use-fixtures]]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as slipstream]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-session-oidc :as oidc]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-session-github :as github]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.configuration-lifecycle-test-utils :as test-utils]))

(use-fixtures :each ltu/with-test-es-client-fixture)
(use-fixtures :once ltu/setup-embedded-zk)

;; initialize must to called to pull in ConfigurationTemplate test examples
#_(dyn/initialize)

;; run tests separately to avoid contamination of the database between tests

(deftest lifecycle-slipstream
  (test-utils/check-lifecycle slipstream/service :registrationEnable false true))

(deftest lifecycle-oidc
  (test-utils/check-lifecycle oidc/service :clientID "server-assigned-client-id" "NEW_ID"))

(deftest lifecycle-github
  (test-utils/check-lifecycle github/service :clientID "github-oauth-application-client-id" "NEW_ID"))

(deftest bad-methods
  (test-utils/check-bad-methods))
