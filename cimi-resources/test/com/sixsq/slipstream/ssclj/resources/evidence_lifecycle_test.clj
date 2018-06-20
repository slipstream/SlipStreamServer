(ns com.sixsq.slipstream.ssclj.resources.evidence-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :as sn]
    [com.sixsq.slipstream.ssclj.resources.evidence :refer :all]
    [peridot.core :refer :all]
    [ring.util.codec :as rc]))

(use-fixtures :each t/with-test-server-fixture)

(def base-uri (str p/service-context resource-url))

(def valid-entry
  {:passed      true
   :planID      "abcd"
   :startTime   "1964-08-25T10:00:00.0Z"
   :endTime     "1964-08-25T10:00:00.0Z"
   :class       "className"
   :schema-org:att1 "123.456"})

(def valid-nested-2-levels
  {:passed      true
   :planID      "abcd"
   :startTime   "1964-08-25T10:00:00.0Z"
   :endTime     "1964-08-25T10:00:00.0Z"
   :class       "className"
   :schema-org:att1 {:schema-org:att2 "456"}})

(def valid-nested-entry
  {:passed      true
   :planID      "abcd"
   :startTime   "1964-08-25T10:00:00.0Z"
   :endTime     "1964-08-25T10:00:00.0Z"
   :class       "className"
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

; will be used to crosscheck with the existing namespaces (above)
; it will not exist thus should be rejected
; only schema-org and schema-com are valid and existing (see below)
(def entry-wrong-namespace
  {:passed      true
   :planID      "abcd"
   :startTime   "1964-08-25T10:00:00.0Z"
   :endTime     "1964-08-25T10:00:00.0Z"
   :class       "className"
   :wrong:att1   "123.456"})

(def valid-namespace {:prefix "schema-org"
                      :uri    "https://schema-org/a/b/c.md"})

(def namespace-com {:prefix "schema-com"
                    :uri    "https://avida/dollar"})

(deftest lifecycle

  (let [session-admin-json (-> (session (ltu/ring-app))
                               (content-type "application/json")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-admin-form (-> (session (ltu/ring-app))
                               (content-type "application/x-www-form-urlencoded")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-user (-> (session (ltu/ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ltu/ring-app))
                         (content-type "application/json"))]

    ;; create namespace
    (-> session-admin-json
        (request (str p/service-context sn/resource-url)
                 :request-method :post
                 :body (json/write-str valid-namespace))
        (t/body->edn)
        (t/is-status 201))

    ;; anonymous create should fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-entry))
        (t/body->edn)
        (t/is-status 403))

    ;; anonymous query should also fail
    (-> session-anon
        (request base-uri)
        (t/body->edn)
        (t/is-status 403))

    ;; creation rejected because attribute belongs to unknown namespace
    (-> session-user
        (request base-uri
                 :request-method :post
                 :body (json/write-str entry-wrong-namespace))
        (t/is-status 406))

    ;; adding, retrieving and  deleting entry as user should succeed
    (let [uri (-> session-user
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-entry))
                  (t/body->edn)
                  (t/is-status 201)
                  (t/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      (-> session-user
          (request abs-uri)
          (t/body->edn)
          (t/is-status 200))

      (-> (session (ltu/ring-app))
          (header authn-info-header "jane role1 ADMIN")
          (request abs-uri :request-method :delete)
          (t/body->edn)
          (t/is-status 200)))

    ;; adding as user, retrieving and deleting entry as ADMIN should work
    (let [uri (-> session-user
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-entry))
                  (t/body->edn)
                  (t/is-status 201)
                  (t/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      (-> session-admin-json
          (request abs-uri)
          (t/body->edn)
          (t/is-status 200))

      (-> session-admin-json
          (request abs-uri
                   :request-method :delete)
          (t/body->edn)
          (t/is-status 200))

      ;; try adding invalid entry
      (-> session-admin-json
          (request base-uri
                   :request-method :post
                   :body (json/write-str invalid-entry))
          (t/body->edn)
          (t/is-status 400)))

    ;; add a new entry
    (let [uri (-> session-admin-json
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-entry))
                  (t/body->edn)
                  (t/is-status 201)
                  (t/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      (is uri)

      ;; verify that the new entry is accessible
      (-> session-admin-json
          (request abs-uri)
          (t/body->edn)
          (t/is-status 200)
          (dissoc :acl)                                     ;; ACL added automatically
          (t/does-body-contain valid-entry))

      ;; query to see that entry is listed
      (let [entries (-> session-admin-json
                        (request base-uri)
                        (t/body->edn)
                        (t/is-status 200)
                        (t/is-resource-uri collection-uri)
                        (t/is-count pos?)
                        (t/entries resource-tag))]

        (is ((set (map :id entries)) uri))

        ;; delete the entry
        (-> session-admin-json
            (request abs-uri :request-method :delete)
            (t/body->edn)
            (t/is-status 200))

        ;; ensure that it really is gone
        (-> session-admin-json
            (request abs-uri)
            (t/body->edn)
            (t/is-status 404))))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (do
          (-> (session (ltu/ring-app))
              (request uri
                       :request-method method
                       :body (json/write-str {:dummy "value"}))
              (t/is-status 405)))))))

(deftest uris-as-keys

  (let [session-admin-json (-> (session (ltu/ring-app))
                               (content-type "application/json")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-admin-form (-> (session (ltu/ring-app))
                               (content-type "application/x-www-form-urlencoded")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-user (-> (session (ltu/ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ltu/ring-app))
                         (content-type "application/json"))]

    ;; create namespace
    (-> session-admin-json
        (request (str p/service-context sn/resource-url)
                 :request-method :post
                 :body (json/write-str valid-namespace))
        (t/body->edn)
        (t/is-status 201))

    (let [with-namespaced-key
          (str "{\"planID\":\"abcd\","
               "\"passed\": true,"
               "\"endTime\": \"1964-08-25T10:00:00.0Z\","
               "\"startTime\": \"1964-08-25T10:00:00.0Z\","
               "\"class\": \"className\","
               "\"schema-org:attr-name\":\"123.456\"}")

          uri-of-posted (-> session-user
                            (request base-uri
                                     :request-method :post
                                     :body with-namespaced-key)
                            (t/body->edn)
                            (t/is-status 201)
                            (t/location))

          abs-uri (str p/service-context (u/de-camelcase uri-of-posted))

          doc (-> session-admin-json
                  (request abs-uri)
                  (t/body->edn)
                  (t/is-status 200)
                  (get-in [:response :body]))]

      (is (:schema-org:attr-name doc))
      (is (= "123.456" (:schema-org:attr-name doc))))))

(deftest nested-values

  (let [session-admin-json (-> (session (ltu/ring-app))
                               (content-type "application/json")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-admin-form (-> (session (ltu/ring-app))
                               (content-type "application/x-www-form-urlencoded")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-user (-> (session (ltu/ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ltu/ring-app))
                         (content-type "application/json"))]

    ;; create namespaces
    (-> session-admin-json
        (request (str p/service-context sn/resource-url)
                 :request-method :post
                 :body (json/write-str valid-namespace))
        (t/body->edn)
        (t/is-status 201))

    (-> session-admin-json
        (request (str p/service-context sn/resource-url)
                 :request-method :post
                 :body (json/write-str namespace-com))
        (t/body->edn)
        (t/is-status 201))

    (let [uri (-> session-user
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-nested-entry))
                  (t/body->edn)
                  (t/is-status 201)
                  (t/location))
          abs-uri (str p/service-context (u/de-camelcase uri))

          doc (-> session-admin-json
                  (request abs-uri)
                  (t/body->edn)
                  (t/is-status 200)
                  (get-in [:response :body]))]

      (is (= "enough of nested" (get-in doc [:schema-org:attnested
                                             :schema-com:subnested
                                             :schema-com:subsubnested
                                             :schema-org:subsubsubnested]))))

    (-> session-user
        (request base-uri
                 :request-method :post
                 :body (json/write-str invalid-nested-entry))
        (t/body->edn)
        (t/is-status 406))))


(deftest cimi-filter-namespaced-attributes

  (let [session-admin-json (-> (session (ltu/ring-app))
                               (content-type "application/json")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-admin-form (-> (session (ltu/ring-app))
                               (content-type "application/x-www-form-urlencoded")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-user (-> (session (ltu/ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ltu/ring-app))
                         (content-type "application/json"))]


    ;; create namespaces
    (-> session-admin-json
        (request (str p/service-context sn/resource-url)
                 :request-method :post
                 :body (json/write-str valid-namespace))
        (t/body->edn)
        (t/is-status 201))

    (-> session-user
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

          res-all (-> session-admin-json
                      (request (str p/service-context resource-url))
                      (t/body->edn)
                      (t/is-status 200)
                      (get-in [:response :body]))

          res-ok (-> session-admin-json
                     (request cimi-url-ok)
                     (t/body->edn)
                     (t/is-status 200)
                     (get-in [:response :body]))

          res-empty (-> session-admin-json
                        (request cimi-url-no-result)
                        (t/body->edn)
                        (t/is-status 200)
                        (get-in [:response :body]))]

      (is (= 1 (:count res-all)))
      (is (= 1 (:count res-ok)))
      (is (= 0 (:count res-empty))))))

(deftest cimi-filter-nested-values

  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-admin-form (-> (ltu/ring-app)
                               session
                               (content-type "application/x-www-form-urlencoded")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-admin-json (header session-anon authn-info-header "super ADMIN USER ANON")
        session-user (header session-anon authn-info-header "jane USER ANON")]

    ;; create namespaces
    (-> session-admin-json
        (request (str p/service-context sn/resource-url)
                 :request-method :post
                 :body (json/write-str valid-namespace))
        (t/body->edn)
        (t/is-status 201))

    (-> session-admin-json
        (request (str p/service-context sn/resource-url)
                 :request-method :post
                 :body (json/write-str namespace-com))
        (t/body->edn)
        (t/is-status 201))

    (let [_ (-> session-user
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
          res-ok (-> session-admin-json
                     (request cimi-url-ok)
                     (t/body->edn)
                     (t/is-status 200)
                     (get-in [:response :body]))

          res-ok-put (-> session-admin-json
                         (request cimi-url-ok
                                  :request-method :put)
                         (t/body->edn)
                         (t/is-status 200)
                         (get-in [:response :body]))

          res-ok-put-body (-> session-admin-form
                              (request cimi-url-ok
                                       :request-method :put
                                       :body (rc/form-encode {:$filter "schema-org:att1/schema-org:att2='456'"}))
                              (t/body->edn)
                              (t/is-status 200)
                              (get-in [:response :body]))

          no-result (-> session-admin-json
                        (request cimi-url-no-result)
                        (t/body->edn)
                        (t/is-status 200)
                        (get-in [:response :body]))

          no-result-put (-> session-admin-json
                            (request cimi-url-no-result
                                     :request-method :put)
                            (t/body->edn)
                            (t/is-status 200)
                            (get-in [:response :body]))

          no-result-put-body (-> session-admin-form
                                 (request cimi-url-no-result
                                          :request-method :put
                                          :body (rc/form-encode {:$filter "schema-org:att1/schema-org:att2='xxx'"}))
                                 (t/body->edn)
                                 (t/is-status 200)
                                 (get-in [:response :body]))]
      (is (= 1 (:count res-ok)))
      (is (= 0 (:count no-result)))
      (is (= 1 (:count res-ok-put)))
      (is (= 0 (:count no-result-put)))
      (is (= 1 (:count res-ok-put-body)))
      (is (= 0 (:count no-result-put-body))))))
