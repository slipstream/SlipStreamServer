(ns com.sixsq.slipstream.ssclj.resources.service-attribute-namespace-lifecycle-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each t/with-test-client-fixture)

(def base-uri (str p/service-context resource-url))

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def valid-namespace
  {:prefix    "schema-org"
   :uri       "https://schema-org/a/b/c.md"})

(def namespace-same-prefix
  {:prefix    "schema-org"
   :uri       "https://schema-com/z"})
(def namespace-same-uri
  {:prefix    "schema-com"
   :uri       "https://schema-org/a/b/c.md"})
(def another-valid-namespace
  {:prefix    "schema-com"
   :uri       "https://schema-com/z"})

(deftest lifecycle
  ;; anonymous create should fail
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-namespace))
      (t/body->edn)
      (t/is-status 403))

  ;; user create should fail
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane USER ANON")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-namespace))
      (t/body->edn)
      (t/is-status 403))

  (let [uri (-> (session (ring-app))
                (content-type "application/json")
                (header authn-info-header "super ADMIN")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-namespace))
                (t/body->edn)
                (t/is-status 201)
                (t/location))
        abs-uri (str p/service-context (u/de-camelcase uri))
        doc (-> (session (ring-app))
                (header authn-info-header "slipstream USER")
                (request abs-uri)
                (t/body->edn)
                (t/is-status 200)
                (get-in [:response :body]))]

    (is (= "schema-org" (:prefix doc)))
    (is (= "https://schema-org/a/b/c.md" (:uri doc)))
    (is (= "service-attribute-namespace/schema-org" uri))

    (-> (session (ring-app))
        (header authn-info-header "jane")
        (request "/api/service-attribute-namespace")
        (t/body->edn)
        (t/is-status 200)
        (get-in [:response :body]))

    ;; trying to create another namespace with same name is forbidden
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "super ADMIN")
        (request base-uri
                 :request-method :post
                 :body (json/write-str namespace-same-prefix))
        (t/body->edn)
        (t/is-status 409)
        (get-in [:response :body :message])
        (= (str "Conflict for " uri))
        is)

    ;; trying to create another namespace with same uri is forbidden
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "super ADMIN")
        (request base-uri
                 :request-method :post
                 :body (json/write-str namespace-same-uri))
        (t/body->edn)
        (t/is-status 409)
        (get-in [:response :body :message])
        (= (str "Conflict for " uri))
        is)

    ;; trying to create another namespace with other name and URI is ok
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "super ADMIN")
        (request base-uri
                 :request-method :post
                 :body (json/write-str another-valid-namespace))
        (t/body->edn)
        (t/is-status 201))

    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri :request-method :delete)
        (t/body->edn)
        (t/is-status 200))))
