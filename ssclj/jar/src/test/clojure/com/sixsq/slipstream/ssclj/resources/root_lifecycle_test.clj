(ns com.sixsq.slipstream.ssclj.resources.root-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.root :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.es.es-util :as esu]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(use-fixtures :each t/flush-db-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn ring-app []
  (t/make-ring-app (t/concat-routes [routes])))

(deftest lifecycle

  ;; ensure that database implementation has been bound
  (ring-app)

  ;; initialize the root resource
  (let [response (add)]
    (is (= 201 (:status response)))
    (is (= (str resource-url "/1") (-> response
                                       :headers
                                       (get "Location"))))
    (is (:body response))

  ;; retrieve root resource (anonymously should work)
  (-> (session (ring-app))
      (request base-uri)
      (t/body->json)
      (t/is-status 200)
      (t/is-resource-uri resource-uri)
      (t/is-operation-absent "edit")
      (t/is-operation-absent "delete"))

  ;; retrieve root resource (root should have edit rights)
  (-> (session (ring-app))
      (header authn-info-header "root ADMIN")
      (request base-uri)
      (t/body->json)
      (t/is-status 200)
      (t/is-resource-uri resource-uri)
      (t/is-operation-present "edit")
      (t/is-operation-absent "delete"))

  ;; updating root resource as user should fail
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane-updater")
      (request base-uri
               :request-method :put
               :body (json/write-str {:name "dummy"}))
      (t/body->json)
      (t/is-status 403))

  ;; update the entry, verify updated doc is returned
  ;; must be done as administrator
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "root ADMIN")
      (request base-uri
               :request-method :put
               :body (json/write-str {:name "dummy"}))
      (t/body->json)
      (t/is-status 200)
      (t/is-resource-uri resource-uri)
      (t/is-operation-present "edit")
      (t/is-key-value :name "dummy"))

  ;; verify that subsequent reads find the right data
  (-> (session (ring-app))
      (request base-uri)
      (t/body->json)
      (t/is-status 200)
      (t/is-resource-uri resource-uri)
      (t/is-operation-absent "edit")
      (t/is-key-value :name "dummy"))))

(deftest bad-methods
  (doall
    (for [[uri method] [[base-uri :options]
                        [base-uri :delete]
                        [base-uri :post]]]
      (-> (session (ring-app))
          (request uri
                   :request-method method
                   :body (json/write-str {:dummy "value"}))
          (t/is-status 405)))))


