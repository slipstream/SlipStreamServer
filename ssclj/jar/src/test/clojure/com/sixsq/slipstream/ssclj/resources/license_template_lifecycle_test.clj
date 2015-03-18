(ns com.sixsq.slipstream.ssclj.resources.license-template-lifecycle-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.license-template :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.db.filesystem-test-utils :as db]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each db/flush-db-fixture)

(use-fixtures :once db/temp-db-fixture)

(def base-uri (str c/service-context resource-name))

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def valid-entry
  {:licenseData "BASE64_LICENSE_DATA"})

(deftest lifecycle

  ;; anonymous create should fail
  (-> (session (ring-app))
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/body->json)
      (t/is-status 403))

  ;; anonymous query should also fail
  (-> (session (ring-app))
      (request base-uri)
      (t/body->json)
      (t/is-status 403))

  ;; adding by user is not allowed
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/body->json)
      (t/is-status 403))

  ;; viewing and modifications only allowed by ADMIN (for now)
  (let [uri (-> (session (ring-app))
                (content-type "application/json")
                (header authn-info-header "root ADMIN")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/body->json)
                (t/is-status 201)
                (t/location))
        abs-uri (str c/service-context uri)]

    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri)
        (t/body->json)
        (t/is-status 200))

    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri
                 :request-method :delete)
        (t/body->json)
        (t/is-status 204)))

  ;; try adding invalid entry
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "root ADMIN")
      (request base-uri
               :request-method :post
               :body (json/write-str (assoc valid-entry :invalid "BAD")))
      (t/body->json)
      (t/is-status 400))

  ;; add a new entry
  (let [uri (-> (session (ring-app))
                (content-type "application/json")
                (header authn-info-header "root ADMIN")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/body->json)
                (t/is-status 201)
                (t/location))
        abs-uri (str c/service-context uri)]

    (is uri)

    ;; verify that the new entry is accessible
    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri)
        (t/body->json)
        (t/is-status 200)
        (dissoc :acl)                                       ;; ACL added automatically
        (t/does-body-contain valid-entry))

    ;; query to see that entry is listed
    (let [entries (-> (session (ring-app))
                      (content-type "application/json")
                      (header authn-info-header "root ADMIN")
                      (request base-uri)
                      (t/body->json)
                      (t/is-status 200)
                      (t/is-resource-uri collection-uri)
                      (t/is-count pos?)
                      (t/entries resource-tag))]
      (is ((set (map :id entries)) uri)))

    ;; delete the entry
    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri
                 :request-method :delete)
        (t/body->json)
        (t/is-status 204))

    ;; ensure that it really is gone
    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri)
        (t/body->json)
        (t/is-status 404))))

(deftest bad-methods
  (let [resource-uri (str c/service-context (u/new-resource-id resource-name))]
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
