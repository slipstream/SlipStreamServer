(ns com.sixsq.slipstream.ssclj.resources.external-object-template-lifecycle-test
  (:require
    [clojure.test :refer [deftest is are use-fixtures]]
    [peridot.core :refer [session header request content-type]]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-alpha-example :as example]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as report]))


(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase eot/resource-name)))


(deftest check-retrieve-by-id
  (let [id (str eot/resource-url "/" example/objectType)
        doc (crud/retrieve-by-id id)]
    (is (= id (:id doc)))))

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


    ;; query as ADMIN should work correctly
    (let [entries (-> session-admin
                      (request base-uri)
                      (ltu/body->edn)
                      (ltu/is-status 200)
                      (ltu/is-resource-uri eot/collection-uri)
                      (ltu/is-count pos?)
                      (ltu/is-operation-absent "add")
                      (ltu/is-operation-absent "delete")
                      (ltu/is-operation-absent "edit")
                      (ltu/is-operation-absent "describe")
                      (ltu/entries eot/resource-tag))
          ids (set (map :id entries))
          types (set (map :objectType entries))]
      (is (= #{(str eot/resource-url "/" example/objectType)
               (str eot/resource-url "/" report/objectType)}
             ids))
      (is (= #{example/objectType
               report/objectType}
             types))

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
          (is (:objectType desc-body))
          (is (:acl desc-body))

          #_(is (thrown-with-msg? ExceptionInfo #".*resource does not satisfy defined schema.*" (crud/validate entry-body)))
          (is (crud/validate entry-body))

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
              (ltu/is-status 200)))))

    ;; query as ADMIN should work correctly
    (let [entries (-> session-admin
                      (request base-uri)
                      (ltu/body->edn)
                      (ltu/is-status 200)
                      (ltu/is-resource-uri eot/collection-uri)
                      (ltu/is-count pos?)
                      (ltu/is-operation-absent "add")
                      (ltu/is-operation-absent "delete")
                      (ltu/is-operation-absent "edit")
                      (ltu/is-operation-absent "describe")
                      (ltu/entries eot/resource-tag))
          ids (set (map :id entries))
          types (set (map :objectType entries))]
      (is (= #{(str eot/resource-url "/" example/objectType)
               (str eot/resource-url "/" report/objectType)}
             ids))
      (is (= #{example/objectType
               report/objectType}
             types))


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
          (is (:objectType desc-body))
          (is (:acl desc-body))


          (is (crud/validate entry-body))

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
