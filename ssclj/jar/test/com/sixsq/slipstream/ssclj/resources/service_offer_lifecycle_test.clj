(ns com.sixsq.slipstream.ssclj.resources.service-offer-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [ring.util.codec :as rc]
    [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :as sn]
    [com.sixsq.slipstream.ssclj.resources.service-offer :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each t/with-test-client-fixture)

(def base-uri (str p/service-context resource-url))

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def valid-entry
  {:connector       {:href "CloudSoftwareSolution1"}
   :schema-org:att1 "123.456"})

(def valid-nested-2-levels
  {:connector       {:href "CloudSoftwareSolution2"}
   :schema-org:att1 {:schema-org:att2 "456"}})

(def valid-nested-entry
  {:connector       {:href "CloudSoftwareSolution3"}
   :schema-org:att1 "hi"
   :schema-org:attnested
                    {:schema-com:subnested
                     {:schema-com:subsubnested
                      {:schema-org:subsubsubnested "enough of nested"}}}})

(def invalid-nested-entry
  (assoc-in valid-nested-entry [:schema-org:attnested
                                :schema-com:subnested
                                :schema-com:subsubnested]
            {:schema-XXX:subsubsubnested "so sad"}))

(def invalid-entry
  {:other "BAD"})

(def entry-wrong-namespace
  {:connector  {:href "CloudSoftwareSolution"}
   :wrong:att1 "123.456"})

(def valid-namespace {:prefix "schema-org"
                      :uri    "https://schema-org/a/b/c.md"})

(def namespace-com {:prefix "schema-com"
                    :uri    "https://avida/dollar"})

(deftest lifecycle
  ;; create namespace
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "super ADMIN")
      (request (str p/service-context sn/resource-url)
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

  ;; creation rejected because attribute belongs to unknown namespace
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request base-uri
               :request-method :post
               :body (json/write-str entry-wrong-namespace))
      (t/is-status 406))

  ;; adding, retrieving and  deleting entry as user should succeed
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
        (header authn-info-header "jane role1 ADMIN")
        (request abs-uri :request-method :delete)
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
          (request abs-uri :request-method :delete)
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
                          [resource-uri :options]
                          [resource-uri :post]]]
        (do
          (-> (session (ring-app))
              (request uri
                       :request-method method
                       :body (json/write-str {:dummy "value"}))
              (t/is-status 405)))))))

(deftest uris-as-keys
  ;; create namespace
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "super ADMIN")
      (request (str p/service-context sn/resource-url)
               :request-method :post
               :body (json/write-str valid-namespace))
      (t/body->edn)
      (t/is-status 201))

  (let [connector-with-namespaced-key
        (str "{\"connector\":{\"href\":\"CloudSoftwareSolution\"},"
             "\"schema-org:attr-name\":\"123.456\"}")

        uri-of-posted (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "jane")
                          (request base-uri
                                   :request-method :post
                                   :body connector-with-namespaced-key)
                          (t/body->edn)
                          (t/is-status 201)
                          (t/location))

        abs-uri (str p/service-context (u/de-camelcase uri-of-posted))

        doc (-> (session (ring-app))
                (header authn-info-header "root ADMIN")
                (request abs-uri)
                (t/body->edn)
                (t/is-status 200)
                (get-in [:response :body]))]

    (is (:schema-org:attr-name doc))
    (is (= "123.456" (:schema-org:attr-name doc)))))

(deftest nested-values
  ;; create namespaces
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "super ADMIN")
      (request (str p/service-context sn/resource-url)
               :request-method :post
               :body (json/write-str valid-namespace))
      (t/body->edn)
      (t/is-status 201))

  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "super ADMIN")
      (request (str p/service-context sn/resource-url)
               :request-method :post
               :body (json/write-str namespace-com))
      (t/body->edn)
      (t/is-status 201))

  (let [uri (-> (session (ring-app))
                (content-type "application/json")
                (header authn-info-header "jane")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-nested-entry))
                (t/body->edn)
                (t/is-status 201)
                (t/location))
        abs-uri (str p/service-context (u/de-camelcase uri))

        doc (-> (session (ring-app))
                (header authn-info-header "root ADMIN")
                (request abs-uri)
                (t/body->edn)
                (t/is-status 200)
                (get-in [:response :body]))]

    (is (= "enough of nested" (get-in doc [:schema-org:attnested
                                           :schema-com:subnested
                                           :schema-com:subsubnested
                                           :schema-org:subsubsubnested]))))

  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request base-uri
               :request-method :post
               :body (json/write-str invalid-nested-entry))
      (t/body->edn)
      (t/is-status 406)))


(deftest cimi-filter-namespaced-attributes
  ;; create namespaces
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "super ADMIN")
      (request (str p/service-context sn/resource-url)
               :request-method :post
               :body (json/write-str valid-namespace))
      (t/body->edn)
      (t/is-status 201))

  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/body->edn)
      (t/is-status 201)
      (t/location))

  (let [cimi-url-ok (str p/service-context
                         resource-url
                         "?$filter=schema-org:att1='123.456'")
        cimi-url-no-result (str p/service-context
                                resource-url
                                "?$filter=schema-org:att1='xxx'")

        res-all (-> (session (ring-app))
                    (header authn-info-header "root ADMIN")
                    (request (str p/service-context resource-url))
                    (t/body->edn)
                    (t/is-status 200)
                    (get-in [:response :body]))

        res-ok (-> (session (ring-app))
                   (header authn-info-header "root ADMIN")
                   (request cimi-url-ok)
                   (t/body->edn)
                   (t/is-status 200)
                   (get-in [:response :body]))

        res-empty (-> (session (ring-app))
                      (header authn-info-header "root ADMIN")
                      (request cimi-url-no-result)
                      (t/body->edn)
                      (t/is-status 200)
                      (get-in [:response :body]))]

    (is (= 1 (:count res-all)))
    (is (= 1 (:count res-ok)))
    (is (= 0 (:count res-empty)))))


(deftest cimi-filter-nested-values
  ;; create namespaces
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "super ADMIN")
      (request (str p/service-context sn/resource-url)
               :request-method :post
               :body (json/write-str valid-namespace))
      (t/body->edn)
      (t/is-status 201))

  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "super ADMIN")
      (request (str p/service-context sn/resource-url)
               :request-method :post
               :body (json/write-str namespace-com))
      (t/body->edn)
      (t/is-status 201))

  (let [_ (-> (session (ring-app))
              (content-type "application/json")
              (header authn-info-header "jane")
              (request base-uri
                       :request-method :post
                       :body (json/write-str valid-nested-2-levels))
              (t/body->edn)
              (t/is-status 201)
              (t/location))
        cimi-url-ok (str p/service-context
                         resource-url
                         "?$filter=schema-org:att1/schema-org:att2='456'")
        cimi-url-no-result (str p/service-context
                                resource-url
                                "?$filter=schema-org:att1/schema-org:att2='xxx'")
        res-ok (-> (session (ring-app))
                   (header authn-info-header "root ADMIN")
                   (request cimi-url-ok)
                   (t/body->edn)
                   (t/is-status 200)
                   (get-in [:response :body]))

        res-ok-put (-> (session (ring-app))
                       (header authn-info-header "root ADMIN")
                       (request cimi-url-ok
                                :request-method :put)
                       (t/body->edn)
                       (t/is-status 200)
                       (get-in [:response :body]))

        res-ok-put-body (-> (session (ring-app))
                            (content-type "application/x-www-form-urlencoded")
                            (header authn-info-header "root ADMIN")
                            (request cimi-url-ok
                                     :request-method :put
                                     :body (rc/form-encode {:$filter "schema-org:att1/schema-org:att2='456'"}))
                            (t/body->edn)
                            (t/is-status 200)
                            (get-in [:response :body]))

        no-result (-> (session (ring-app))
                      (header authn-info-header "root ADMIN")
                      (request cimi-url-no-result)
                      (t/body->edn)
                      (t/is-status 200)
                      (get-in [:response :body]))

        no-result-put (-> (session (ring-app))
                          (header authn-info-header "root ADMIN")
                          (request cimi-url-no-result
                                   :request-method :put)
                          (t/body->edn)
                          (t/is-status 200)
                          (get-in [:response :body]))

        no-result-put (-> (session (ring-app))
                          (content-type "application/x-www-form-urlencoded")
                          (header authn-info-header "root ADMIN")
                          (request cimi-url-no-result
                                   :request-method :put
                                   :body (rc/form-encode {:$filter "schema-org:att1/schema-org:att2='xxx'"}))
                          (t/body->edn)
                          (t/is-status 200)
                          (get-in [:response :body]))
        ]
    (is (= 1 (:count res-ok)))
    (is (= 0 (:count no-result)))
    (is (= 1 (:count res-ok-put)))
    (is (= 0 (:count no-result-put)))
    ))
