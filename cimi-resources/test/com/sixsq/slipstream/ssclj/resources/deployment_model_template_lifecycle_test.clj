(ns com.sixsq.slipstream.ssclj.resources.deployment-model-template-lifecycle-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.deployment-model-template :as dmt]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [peridot.core :refer :all]))


(use-fixtures :each ltu/with-test-server-fixture)


(def base-uri (str p/service-context (u/de-camelcase dmt/resource-name)))


(deftest check-retrieve-by-id
  (let [id (str dmt/resource-url "/standard")
        doc (crud/retrieve-by-id id)]
    (is (= id (:id doc)))))


;; check that all templates are visible as normal user
(deftest lifecycle-admin
  (let [session-user (-> (session (ltu/ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        entries (-> session-user
                    (request base-uri)
                    (ltu/body->edn)
                    (ltu/is-status 200)
                    (ltu/is-resource-uri dmt/collection-uri)
                    (ltu/is-count pos?)
                    (ltu/is-operation-absent "add")
                    (ltu/is-operation-absent "delete")
                    (ltu/is-operation-absent "edit")
                    (ltu/is-operation-absent "describe")
                    (ltu/entries dmt/resource-tag))
        ids (set (map :id entries))

        entry (first entries)]

    (is (= 1 (count entries)))
    (is (= #{(str dmt/resource-url "/standard")} ids))

    (let [ops (ltu/operations->map entry)
          href (get ops (c/action-uri :describe))
          entry-url (str p/service-context (:id entry))
          describe-url (str p/service-context href)

          entry-resp (-> session-user
                         (request entry-url)
                         (ltu/is-status 200)
                         (ltu/body->edn))

          entry-body (get-in entry-resp [:response :body])

          desc (-> session-user
                   (request describe-url)
                   (ltu/body->edn)
                   (ltu/is-status 200))
          desc-body (get-in desc [:response :body])]

      (is (nil? (get ops (c/action-uri :add))))
      (is (nil? (get ops (c/action-uri :edit))))
      (is (nil? (get ops (c/action-uri :delete))))
      (is (:module desc-body))
      (is (:acl desc-body))

      (is (crud/validate entry-body)))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id dmt/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :post]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :put]
                            [resource-uri :post]
                            [resource-uri :delete]])))
