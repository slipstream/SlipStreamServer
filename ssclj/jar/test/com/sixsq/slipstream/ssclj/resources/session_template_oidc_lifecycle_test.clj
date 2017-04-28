(ns com.sixsq.slipstream.ssclj.resources.session-template-oidc-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.set :as set]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.session-template :refer :all]
    [com.sixsq.slipstream.ssclj.resources.session-template-oidc :as oidc]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in SessionTemplate resources
(dyn/initialize)

(deftest check-retrieve-by-id
  (let [id (str resource-url "/" oidc/authn-method)
        doc (crud/retrieve-by-id id)]
    (is (= id (:id doc)))))

(deftest lifecycle

  ;; all view actions should be available to anonymous users
  (let [session (session (ring-app))
        entries (-> session
                    (content-type "application/json")
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
        types (set (map :method entries))]
    (is (set/subset? #{(str resource-url "/" oidc/authn-method)} ids))
    (is (set/subset? #{oidc/authn-method} types))

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
        (is (:method desc-body))
        (is (:acl desc-body))

        (is (crud/validate entry-body))))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :post]
                          [base-uri :delete]
                          [resource-uri :options]
                          [resource-uri :put]
                          [resource-uri :post]
                          [resource-uri :delete]]]
        (-> (session (ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
