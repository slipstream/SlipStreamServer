(ns com.sixsq.slipstream.ssclj.resources.configuration-slipstream-lifecycle-test
  (:require
    [clojure.test :refer [deftest use-fixtures is]]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.configuration :refer :all]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as slipstream]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.configuration-lifecycle-test-utils :as test-utils]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as ct]))

(use-fixtures :each ltu/with-test-server-fixture)


(def base-uri (str p/service-context (u/de-camelcase resource-name)))

;; must have specialized checks for slipstream configuration because
;; the initialization function creates a default slipstream configuration
;; resource

(defn check-existing-configuration
  [service attr-kw attr-value]

  (let [session (-> (ltu/ring-app)
                    session
                    (content-type "application/json"))
        session-admin (header session authn-info-header "root ADMIN USER ANON")

        template-url (str p/service-context ct/resource-url "/" service)
        resp (-> session-admin
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))
        template (get-in resp [:response :body])
        valid-create {:configurationTemplate (ltu/strip-unwanted-attrs (assoc template attr-kw attr-value))}]

    ;; admin create with valid template should fail
    ;; slipstream configuration initialization will have already created a resource
    (-> session-admin
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-create))
        (ltu/body->edn)
        (ltu/is-status 409))

    ;; delete the existing entry, to allow for standard tests afterwards
    (let [uri (str (u/de-camelcase resource-name) "/" service)
          abs-uri (str p/service-context uri)]

      ;; admin delete succeeds
      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))))


(deftest lifecycle-slipstream
  (check-existing-configuration slipstream/service :registrationEnable true)
  (test-utils/check-lifecycle slipstream/service :registrationEnable true false))
