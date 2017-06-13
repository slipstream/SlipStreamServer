(ns com.sixsq.slipstream.ssclj.resources.session-template-cyclone-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.session-template-cyclone :as cyclone]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-template-lifecycle-test-utils :as stu]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase st/resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in SessionTemplate descriptions
(dyn/initialize)

(def valid-template {:method      cyclone/authn-method
                     :methodKey   cyclone/authn-method
                     :name        "CYCLONE"
                     :description "External Authentication via CYCLONE Keycloak"
                     :acl         st/resource-acl})

(deftest lifecycle
  (stu/session-template-lifecycle base-uri (ring-app) valid-template))

(deftest bad-methods
  (stu/bad-methods base-uri (ring-app)))
