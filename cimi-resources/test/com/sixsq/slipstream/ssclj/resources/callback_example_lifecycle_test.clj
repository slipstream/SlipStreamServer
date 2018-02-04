(ns com.sixsq.slipstream.ssclj.resources.callback-example-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback-example :as example]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase callback/resource-url)))

(deftest lifecycle
  (let [session (-> (ltu/ring-app)
                    session
                    (content-type "application/json"))
        session-admin (header session authn-info-header "root ADMIN USER ANON")
        session-anon (header session authn-info-header "unknown ANON")]

    ;; create a callback as an admin
    (let [create-callback-succeeds {:action   example/action-name
                                    :resource {:href "example/resource-x"}
                                    :data     {:ok? true}}

          create-callback-fails {:action   example/action-name
                                 :resource {:href "example/resource-y"}
                                 :data     {:ok? false}}

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

          uri-fails (str p/service-context (-> session-admin
                                               (request base-uri
                                                        :request-method :post
                                                        :body (json/write-str create-callback-fails))
                                               (ltu/body->edn)
                                               (ltu/is-status 201)
                                               :response
                                               :body
                                               :resource-id))

          trigger-fails (str p/service-context (-> session-admin
                                                   (request uri-fails)
                                                   (ltu/body->edn)
                                                   (ltu/is-status 200)
                                                   (ltu/get-op "execute")))]

      ;; anon should be able to trigger the callbacks
      (-> session-anon
          (request trigger-succeeds)
          (ltu/body->edn)
          (ltu/is-status 200))

      (-> session-anon
          (request trigger-fails)
          (ltu/body->edn)
          (ltu/is-status 400))

      ;; retriggering the callbacks must fail with 409
      (-> session-anon
          (request trigger-succeeds)
          (ltu/body->edn)
          (ltu/is-status 409))

      (-> session-anon
          (request trigger-fails)
          (ltu/body->edn)
          (ltu/is-status 409))

      ;; delete
      (-> session-admin
          (request uri-succeeds
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      (-> session-admin
          (request uri-fails
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure that an unknown callback returns 404
      (-> session-anon
          (request trigger-succeeds)
          (ltu/body->edn)
          (ltu/is-status 404))

      )))
