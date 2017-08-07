(ns com.sixsq.slipstream.ssclj.resources.session-lifecycle-test
  (:require
    [clojure.test :refer [use-fixtures deftest]]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.session :as session]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase session/resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in SessionTemplate test examples
(dyn/initialize)

(deftest bad-methods
    (let [resource-uri (str p/service-context (u/new-resource-id session/resource-name))]
      (doall
        (for [[uri method] [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :put]
                            [resource-uri :post]]]
          (-> (session (ring-app))
              (request uri
                       :request-method method
                       :body (json/write-str {:dummy "value"}))
              (ltu/is-status 405))))))
