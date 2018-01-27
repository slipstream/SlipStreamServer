(ns com.sixsq.slipstream.ssclj.resources.job-lifecycle-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.job :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [com.sixsq.slipstream.ssclj.resources.job.utils :as ju]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context resource-url))

(def valid-job
  {:resourceURI resource-uri
   :action      "collect"
   :acl         {:owner {:type "USER" :principal "admin"}
                 :rules [{:type "USER" :principal "jane" :right "VIEW"}]}})

(deftest lifecycle
  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-admin (header session-anon authn-info-header "super ADMIN USER ANON")
        session-user (header session-anon authn-info-header "jane USER ANON")]

    (initialize)

    (is (uzk/exists ju/locking-queue-path))

    ;; anonymous create should fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-job))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; user create should fail
    (-> session-user
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-job))
        (ltu/body->edn)
        (ltu/is-status 403))

    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-job))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))
          job (-> session-user
                  (request abs-uri)
                  (ltu/body->edn)
                  (ltu/is-status 200)
                  (ltu/is-operation-present "stop")
                  (get-in [:response :body]))]

      (is (= "QUEUED" (:state job)))

      (is (= (uzk/get-data "/job/entries/entry-100-0000000000") uri))

      (-> session-user
          (request "/api/job")
          (ltu/body->edn)
          (ltu/is-status 200)
          (get-in [:response :body]))

      ;; set state to a final state make progress to set 100 automatically
      (is (= 100 (-> session-admin
                     (request abs-uri :request-method :put
                              :body (json/write-str {:state "SUCCESS"}))
                     (ltu/body->edn)
                     (ltu/is-status 200)
                     (get-in [:response :body :progress]))))

      (-> session-admin
          (request abs-uri :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))))
