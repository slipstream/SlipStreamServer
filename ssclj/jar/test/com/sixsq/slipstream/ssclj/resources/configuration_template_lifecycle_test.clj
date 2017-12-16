(ns com.sixsq.slipstream.ssclj.resources.configuration-template-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as slipstream]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-session-oidc :as oidc]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-session-github :as github]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-lifecycle-test-utils :as test-utils]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase ct/resource-name)))

;; initialize must to called to pull in ConfigurationTemplate resources
(dyn/initialize)

(deftest retrieve-by-id
  (test-utils/check-retrieve-by-id slipstream/service)
  (test-utils/check-retrieve-by-id oidc/service)
  (test-utils/check-retrieve-by-id github/service))

(deftest lifecycle
  (test-utils/check-lifecycle slipstream/service)
  (test-utils/check-lifecycle oidc/service)
  (test-utils/check-lifecycle github/service))

(deftest bad-methods
  (test-utils/check-bad-methods))
