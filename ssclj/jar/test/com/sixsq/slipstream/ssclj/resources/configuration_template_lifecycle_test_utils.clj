(ns com.sixsq.slipstream.ssclj.resources.configuration-template-lifecycle-test-utils
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :refer :all]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as slipstream]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

;; initialize must to called to pull in ConfigurationTemplate resources
(dyn/initialize)

(defn check-retrieve-by-id
  [service]
  (let [id (str resource-url "/" service)
        doc (crud/retrieve-by-id id)]
    (is (= id (:id doc)))))

(defn check-lifecycle
  [service]

  ;; anonymous query is not authorized
  (-> (session (ltu/ring-app))
      (request base-uri)
      (ltu/body->edn)
      (ltu/is-status 403))

  ;; user query is not authorized
  (-> (session (ltu/ring-app))
      (header authn-info-header "jane USER")
      (request base-uri)
      (ltu/body->edn)
      (ltu/is-status 403))

  ;; query as ADMIN should work correctly
  (let [session (session (ltu/ring-app))
        entries (-> session
                    (content-type "application/json")
                    (header authn-info-header "root ADMIN")
                    (request base-uri)
                    (ltu/body->edn)
                    (ltu/is-status 200)
                    (ltu/is-resource-uri collection-uri)
                    (ltu/is-count pos?)
                    (ltu/is-operation-absent "add")
                    (ltu/is-operation-absent "delete")
                    (ltu/is-operation-absent "edit")
                    (ltu/is-operation-absent "describe")
                    (ltu/entries resource-tag))
        ids (set (map :id entries))
        types (set (map :service entries))]
    (is (contains? ids (str resource-url "/" service)))
    (is (contains? types service))

    (doseq [entry entries]
      (let [ops (ltu/operations->map entry)
            href (get ops (c/action-uri :describe))
            entry-url (str p/service-context (:id entry))
            describe-url (str p/service-context href)

            entry-resp (-> session
                           (content-type "application/json")
                           (header authn-info-header "root ADMIN")
                           (request entry-url)
                           (ltu/is-status 200)
                           (ltu/body->edn))

            entry-body (get-in entry-resp [:response :body])

            desc (-> session
                     (content-type "application/json")
                     (header authn-info-header "root ADMIN")
                     (request describe-url)
                     (ltu/body->edn)
                     (ltu/is-status 200))
            desc-body (get-in desc [:response :body])]
        (is (nil? (get ops (c/action-uri :add))))
        (is (nil? (get ops (c/action-uri :edit))))
        (is (nil? (get ops (c/action-uri :delete))))
        (is (:service desc-body))
        (is (:acl desc-body))

        (is (crud/validate entry-body))

        ;; anonymous access not permitted
        (-> session
            (content-type "application/json")
            (request entry-url)
            (ltu/is-status 403))
        (-> session
            (content-type "application/json")
            (request describe-url)
            (ltu/is-status 403))

        ;; user cannot access
        (-> session
            (content-type "application/json")
            (header authn-info-header "jane USER")
            (request entry-url)
            (ltu/is-status 403))
        (-> session
            (content-type "application/json")
            (header authn-info-header "jane USER")
            (request describe-url)
            (ltu/is-status 403))))))

(defn check-bad-methods
  []
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :post]
                          [base-uri :delete]
                          [resource-uri :options]
                          [resource-uri :put]
                          [resource-uri :post]
                          [resource-uri :delete]]]
        (-> (session (ltu/ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
