(ns com.sixsq.slipstream.ssclj.resources.user-params-template-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.user-params-template :as upt]
    [com.sixsq.slipstream.ssclj.resources.user-params-template-exec :as exec]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :each ltu/with-test-es-client-fixture)
(use-fixtures :once ltu/setup-embedded-zk)

(def base-uri (str p/service-context (u/de-camelcase upt/resource-name)))

;; initialize must to called to pull in ConnectorTemplate test examples
#_(dyn/initialize)

(deftest lifecycle

  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-user (header session-anon authn-info-header "jane USER")
        session-admin (header session-anon authn-info-header "root ADMIN")]

    ;; anonymous query is not authorized
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; user query is authorized
    (-> session-user
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200))

    ;; query as ADMIN and USER should work correctly
    (let [session (session (ltu/ring-app))
          entries (-> session-admin
                      (request base-uri)
                      (ltu/body->edn)
                      (ltu/is-status 200)
                      (ltu/is-resource-uri upt/collection-uri)
                      (ltu/is-count pos?)
                      (ltu/is-operation-absent "add")
                      (ltu/is-operation-absent "delete")
                      (ltu/is-operation-absent "edit")
                      (ltu/is-operation-absent "describe")
                      (ltu/entries upt/resource-tag))
          ids (set (map :id entries))
          types (set (map :paramsType entries))]
      (is (pos-int? (count ids)))
      (is (pos-int? (count types)))
      (is (= #{(str upt/resource-url "/" exec/params-type)} ids))
      (is (= #{exec/params-type} types))

      (doseq [entry entries]
        (let [ops (ltu/operations->map entry)
              href (get ops (c/action-uri :describe))
              entry-url (str p/service-context (:id entry))
              describe-url (str p/service-context href)

              entry-resp (-> session-admin
                             (request entry-url)
                             (ltu/is-status 200)
                             (ltu/body->edn))

              entry-body (get-in entry-resp [:response :body])
              desc (-> session-admin
                       (request describe-url)
                       (ltu/body->edn)
                       (ltu/is-status 200))
              desc-body (get-in desc [:response :body])]
          (is (nil? (get ops (c/action-uri :add))))
          (is (nil? (get ops (c/action-uri :edit))))
          (is (nil? (get ops (c/action-uri :delete))))
          (is (not (nil? desc-body)))
          (is (:paramsType desc-body))
          (is (:acl desc-body))

          ;; anonymous access not permitted
          (-> session-anon
              (request entry-url)
              (ltu/is-status 403))
          (-> session-anon
              (request describe-url)
              (ltu/is-status 403))

          ;; user can access
          (-> session-user
              (request entry-url)
              (ltu/is-status 200))
          (-> session-user
              (request describe-url)
              (ltu/is-status 200)))))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id upt/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :post]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :put]
                            [resource-uri :post]
                            [resource-uri :delete]])))
