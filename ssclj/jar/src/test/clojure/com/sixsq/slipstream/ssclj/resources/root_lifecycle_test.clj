(ns com.sixsq.slipstream.ssclj.resources.root-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.root :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.es.es-util :as esu]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [routes])))

(deftest lifecycle

  ;; ensure that database implementation has been bound
  (ring-app)

  ;; initialize the root resource
  (let [response (add)]
    (is (= 201 (:status response)))
    (is (= (str resource-url) (-> response
                                  :headers
                                  (get "Location"))))
    (is (:body response))

  ; retrieve root resource (anonymously should work)
  (-> (session (ring-app))
      (request base-uri)
      (ltu/body->json)
      (ltu/is-status 200)
      (ltu/is-resource-uri resource-uri)
      (ltu/is-operation-absent "edit")
      (ltu/is-operation-absent "delete"))

  ;; retrieve root resource (root should have edit rights)
  (-> (session (ring-app))
      (header authn-info-header "root ADMIN")
      (request base-uri)
      (ltu/body->json)
      (ltu/is-status 200)
      (ltu/is-resource-uri resource-uri)
      (ltu/is-operation-present "edit")
      (ltu/is-operation-absent "delete"))

  ;; updating root resource as user should fail
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane-updater")
      (request base-uri
               :request-method :put
               :body (json/write-str {:name "dummy"}))
      (ltu/body->json)
      (ltu/is-status 403))

  ;; update the entry, verify updated doc is returned
  ;; must be done as administrator
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "root ADMIN")
      (request base-uri
               :request-method :put
               :body (json/write-str {:name "dummy"}))
      (ltu/body->json)
      (ltu/is-status 200)
      (ltu/is-resource-uri resource-uri)
      (ltu/is-operation-present "edit")
      (ltu/is-key-value :name "dummy"))

  ;; verify that subsequent reads find the right data
  (-> (session (ring-app))
      (request base-uri)
      (ltu/body->json)
      (ltu/is-status 200)
      (ltu/is-resource-uri resource-uri)
      (ltu/is-operation-absent "edit")
      (ltu/is-key-value :name "dummy"))))

(deftest bad-methods
  (doall
    (for [[uri method] [[base-uri :options]
                        [base-uri :delete]
                        [base-uri :post]]]
      (-> (session (ring-app))
          (request uri
                   :request-method method
                   :body (json/write-str {:dummy "value"}))
          (ltu/is-status 405)))))



