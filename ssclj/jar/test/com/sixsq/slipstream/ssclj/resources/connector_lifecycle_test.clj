(ns com.sixsq.slipstream.ssclj.resources.connector-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.connector :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.es.es-util :as esu]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

(def valid-entry
  {:serviceName "CloudSoftwareSolution"})

(def valid-create-entry
  {:connectorTemplate valid-entry})

(def invalid-create-entry
  {:connectorTemplate
   (assoc valid-entry :invalid "BAD")})

(deftest lifecycle

  ;; anonymous create should fail
  (-> (session (ring-app))
      (content-type "application/json")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-create-entry))
      (ltu/body->json)
      (ltu/is-status 403))

  ;; anonymous query should also fail
  (-> (session (ring-app))
      (request base-uri)
      (ltu/body->json)
      (ltu/is-status 403))

  ;; adding, retrieving and  deleting entry as user should succeed
  (let [uri     (-> (session (ring-app))
                    (content-type "application/json")
                    (header authn-info-header "jane")
                    (request base-uri
                             :request-method :post
                             :body (json/write-str valid-create-entry))
                    (ltu/body->json)
                    (ltu/is-status 201)
                    (ltu/location))
        abs-uri (str p/service-context (u/de-camelcase uri))]

    (-> (session (ring-app))
        (header authn-info-header "jane")
        (request abs-uri)
        (ltu/body->json)
        (ltu/is-status 200))

    (-> (session (ring-app))
        (header authn-info-header "jane")
        (request abs-uri
                 :request-method :delete)
        (ltu/body->json)
        (ltu/is-status 200)))

  ;; adding as user, retrieving and deleting entry as ADMIN should work
  (let [uri     (-> (session (ring-app))
                    (content-type "application/json")
                    (header authn-info-header "jane")
                    (request base-uri
                             :request-method :post
                             :body (json/write-str valid-create-entry))
                    (ltu/body->json)
                    (ltu/is-status 201)
                    (ltu/location))
        abs-uri (str p/service-context (u/de-camelcase uri))]

    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri)
        (ltu/body->json)
        (ltu/is-status 200))

    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri
                 :request-method :delete)
        (ltu/body->json)
        (ltu/is-status 200))

    ;; try adding invalid entry
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "root ADMIN")
        (request base-uri
                 :request-method :post
                 :body (json/write-str invalid-create-entry))
        (ltu/body->json)
        (ltu/is-status 400)))

  ;; add a new entry
  (let [uri     (-> (session (ring-app))
                    (content-type "application/json")
                    (header authn-info-header "root ADMIN")
                    (request base-uri
                             :request-method :post
                             :body (json/write-str valid-create-entry))
                    (ltu/body->json)
                    (ltu/is-status 201)
                    (ltu/location))
        abs-uri (str p/service-context (u/de-camelcase uri))]

    (is uri)

    ;; verify that the new entry is accessible
    (-> (session (ring-app))
        (header authn-info-header "root ADMIN")
        (request abs-uri)
        (ltu/body->json)
        (ltu/is-status 200)
        (dissoc :acl)                                       ;; ACL added automatically
        (ltu/does-body-contain valid-entry))

    ;; query to see that entry is listed
    (let [entries (-> (session (ring-app))
                      (content-type "application/json")
                      (header authn-info-header "root ADMIN")
                      (request base-uri)
                      (ltu/body->json)
                      (ltu/is-status 200)
                      (ltu/is-resource-uri collection-uri)
                      (ltu/is-count pos?)
                      (ltu/entries resource-tag))]
      (is ((set (map :id entries)) uri))

      ;; delete the entry
      (-> (session (ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri
                   :request-method :delete)
          (ltu/body->json)
          (ltu/is-status 200))

      ;; ensure that it really is gone
      (-> (session (ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (ltu/body->json)
          (ltu/is-status 404)))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [base-uri :put]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (-> (session (ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
