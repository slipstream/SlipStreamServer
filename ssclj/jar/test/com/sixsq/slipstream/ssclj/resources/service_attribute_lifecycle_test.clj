  (ns com.sixsq.slipstream.ssclj.resources.service-attribute-lifecycle-test
    (:require
      [clojure.test :refer :all]
      [peridot.core :refer :all]
      [clojure.data.json :as json]
      [com.sixsq.slipstream.ssclj.resources.service-attribute :refer :all]
      [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
      [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
      [com.sixsq.slipstream.ssclj.app.routes :as routes]
      [com.sixsq.slipstream.ssclj.app.params :as p]
      [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
      [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :as san]))

(use-fixtures :each t/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def valid-entry
  {
   :prefix        "example-org"
   :attr-name     "test-attribute"

   :type          "string"
   :authority     "http://helix-nebula.eu"
   :major-version 2
   :minor-version 1
   :patch-version 0
   :normative     true
   :en            {:name        "keyword"
                   :description "localized description"
                   :categories  ["one" "two"]}})

(def invalid-entry
  (merge valid-entry {:other "BAD"}))

(def valid-namespace
  {:prefix    "example-org"
   :uri       "https://schema-org/a/b/c.md"})

(deftest lifecycle

  ;; create namespace
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "super ADMIN")
      (request (str p/service-context san/resource-url)
               :request-method :post
               :body (json/write-str valid-namespace))
      (t/body->edn)
      (t/is-status 201))

  ;; anonymous create should fail
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/body->edn)
      (t/is-status 403))

  ;; anonymous query should also fail
  (-> (session (ring-app))
      (request base-uri)
      (t/body->edn)
      (t/is-status 403))

  ; adding the same attribute twice should fail
  (let [uri (-> (session (ring-app))
                (content-type "application/json")
                (header authn-info-header "jane")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/body->edn)
                (t/is-status 201)
                (t/location))
        abs-uri (str p/service-context (u/de-camelcase uri))]


    (-> (session (ring-app))
        (header authn-info-header "jane")
        (request abs-uri)
        (t/body->edn)
        (t/is-status 200))

    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "jane")
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-entry))
        (t/body->edn)
        (t/is-status 409))

    (-> (session (ring-app))
        (header authn-info-header "jane")
        (request abs-uri :request-method :delete)
        (t/body->edn)
        (t/is-status 200)))

  ;; adding, retrieving and deleting entry as user should succeed
  (let [uri (-> (session (ring-app))
                (content-type "application/json")
                (header authn-info-header "jane")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/body->edn)
                (t/is-status 201)
                (t/location))
        abs-uri (str p/service-context (u/de-camelcase uri))]

    (-> (session (ring-app))
        (header authn-info-header "jane")
        (request abs-uri)
        (t/body->edn)
        (t/is-status 200))

    (-> (session (ring-app))
        (header authn-info-header "jane")
        (request abs-uri
                 :request-method :delete)
        (t/body->edn)
        (t/is-status 200)))

  ;; adding as user, retrieving and deleting entry as ADMIN should work
  (let [uri (-> (session (ring-app))
                (content-type "application/json")
                (header authn-info-header "jane")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/body->edn)
                (t/is-status 201)
                (t/location))
        abs-uri (str p/service-context (u/de-camelcase uri))]

    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri)
        (t/body->edn)
        (t/is-status 200))

    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri
                 :request-method :delete)
        (t/body->edn)
        (t/is-status 200))

    ;; try adding invalid entry
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "root ADMIN")
        (request base-uri
                 :request-method :post
                 :body (json/write-str invalid-entry))
        (t/body->edn)
        (t/is-status 400)))

  ;; add a new entry
  (let [uri (-> (session (ring-app))
                (content-type "application/json")
                (header authn-info-header "root ADMIN")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/body->edn)
                (t/is-status 201)
                (t/location))
        abs-uri (str p/service-context (u/de-camelcase uri))]

    (is uri)

    ;; verify that the new entry is accessible
    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri)
        (t/body->edn)
        (t/is-status 200)
        (dissoc :acl)                                       ;; ACL added automatically
        (t/does-body-contain valid-entry))

    ;; query to see that entry is listed
    (let [entries (-> (session (ring-app))
                      (content-type "application/json")
                      (header authn-info-header "root ADMIN")
                      (request base-uri)
                      (t/body->edn)
                      (t/is-status 200)
                      (t/is-resource-uri collection-uri)
                      (t/is-count pos?)
                      (t/entries resource-tag))]

       (is ((set (map :id entries)) uri))

      ;; delete the entry
      (-> (session (ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri
                   :request-method :delete)
          (t/body->edn)
          (t/is-status 200))

      ;; ensure that it really is gone
      (-> (session (ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (t/body->edn)
          (t/is-status 404)))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [base-uri :put]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (do
          (-> (session (ring-app))
              (request uri
                       :request-method method
                       :body (json/write-str {:dummy "value"}))
              (t/is-status 405)))))))
