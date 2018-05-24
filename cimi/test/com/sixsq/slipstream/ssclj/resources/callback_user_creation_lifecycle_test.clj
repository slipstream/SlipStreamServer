(ns com.sixsq.slipstream.ssclj.resources.callback-user-creation-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback-user-creation :as user]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [peridot.core :refer :all]))


(use-fixtures :each ltu/with-test-server-fixture)


(def base-uri (str p/service-context (u/de-camelcase callback/resource-url)))


(deftest lifecycle
  (let [session (-> (ltu/ring-app)
                    session
                    (content-type "application/json"))
        session-admin (header session authn-info-header "root ADMIN USER ANON")
        session-anon (header session authn-info-header "unknown ANON")]

    ;; create a callback as an admin
    (let [create-callback-succeeds {:action         user/action-name
                                    :targetResource {:href "example/test-user"}
                                    :data           {:key1 "key1"
                                                     :key2 "key2"}}

          uri-succeeds (str p/service-context (-> session-admin
                                                  (request base-uri
                                                           :request-method :post
                                                           :body (json/write-str create-callback-succeeds))
                                                  (ltu/body->edn)
                                                  (ltu/is-status 201)
                                                  :response
                                                  :body
                                                  :resource-id))

          trigger-succeeds (str p/service-context (-> session-admin
                                                      (request uri-succeeds)
                                                      (ltu/body->edn)
                                                      (ltu/is-status 200)
                                                      (ltu/get-op "execute")))

          uri-user (str p/service-context "user/test-username")]

      ;; created user should not exist
      (-> session-admin
          (request uri-user)
          (ltu/body->edn)
          (ltu/is-status 404))

      ;; anon should be able to trigger the callbacks
      (-> session-anon
          (request trigger-succeeds)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; created user should now exist!
      (-> session-admin
          (request uri-user)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; retriggering the callbacks must fail with 409
      (-> session-anon
          (request trigger-succeeds)
          (ltu/body->edn)
          (ltu/is-status 409))

      ;; delete
      (-> session-admin
          (request uri-succeeds
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure that an unknown callback returns 404
      (-> session-anon
          (request trigger-succeeds)
          (ltu/body->edn)
          (ltu/is-status 404)))))
