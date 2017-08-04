(ns com.sixsq.slipstream.ssclj.resources.credential-template-lifecycle-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.credential-template-username-password :as upc]
    [com.sixsq.slipstream.ssclj.resources.credential-template-ssh-public-key :as spk]
    [com.sixsq.slipstream.ssclj.resources.credential-template-ssh-key-pair :as skp]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase ct/resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

;; initialize must to called to pull in CredentialTemplate resources
(dyn/initialize)

(deftest check-retrieve-by-id
  (doseq [registration-method [upc/credential-type
                               spk/credential-type
                               skp/credential-type]]
    (let [id (str ct/resource-url "/" registration-method)
          doc (crud/retrieve-by-id id)]
      (is (= id (:id doc))))))

;; check that all templates are visible as normal user
(deftest lifecycle-admin
  (let [session (-> (session (ring-app))
                    (content-type "application/json")
                    (header authn-info-header "jane USER ANON"))
        entries (-> session
                    (request base-uri)
                    (ltu/body->edn)
                    (ltu/is-status 200)
                    (ltu/is-resource-uri ct/collection-uri)
                    (ltu/is-count pos?)
                    (ltu/is-operation-absent "add")
                    (ltu/is-operation-absent "delete")
                    (ltu/is-operation-absent "edit")
                    (ltu/is-operation-absent "describe")
                    (ltu/entries ct/resource-tag))
        ids (set (map :id entries))
        types (set (map :type entries))]
    (is (= #{(str ct/resource-url "/" upc/credential-type)
             (str ct/resource-url "/" spk/credential-type)
             (str ct/resource-url "/" skp/credential-type)}
           ids))
    (is (= #{upc/credential-type
             spk/credential-type
             skp/credential-type}
           types))

    (doseq [entry entries]
      (let [ops (ltu/operations->map entry)
            href (get ops (c/action-uri :describe))
            entry-url (str p/service-context (:id entry))
            describe-url (str p/service-context href)

            entry-resp (-> session
                           (request entry-url)
                           (ltu/is-status 200)
                           (ltu/body->edn))

            entry-body (get-in entry-resp [:response :body])

            desc (-> session
                     (request describe-url)
                     (ltu/body->edn)
                     (ltu/is-status 200))
            desc-body (get-in desc [:response :body])]
        (is (nil? (get ops (c/action-uri :add))))
        (is (nil? (get ops (c/action-uri :edit))))
        (is (nil? (get ops (c/action-uri :delete))))
        (is (:type desc-body))
        (is (:acl desc-body))

        (is (crud/validate entry-body))))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id ct/resource-name))]
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
