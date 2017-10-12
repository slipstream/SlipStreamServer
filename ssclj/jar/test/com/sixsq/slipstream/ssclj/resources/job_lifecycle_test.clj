(ns com.sixsq.slipstream.ssclj.resources.job-lifecycle-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.job :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]))

(use-fixtures :each t/with-test-es-client-fixture)

(def base-uri (str p/service-context resource-url))

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def valid-job
  {:id          (str resource-url "/test-quota")
   :resourceURI resource-uri
   :progress    0
   :action      "add"
   :targetResource {:href "abc/def"}
   :affectedResources [{:href "abc/def"}]
   :acl     {:owner {:type "USER" :principal "admin"}
             :rules [{:type "USER" :principal "jane" :right "VIEW"}]}})

(deftest lifecycle
  (let [session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "super ADMIN USER ANON"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json"))]

    ;; anonymous create should fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-job))
        (t/body->edn)
        (t/is-status 403))

    ;; user create should fail
    (-> session-user
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-job))
        (t/body->edn)
        (t/is-status 403))

    (let [uri (-> session-admin
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-job))
                  (t/body->edn)
                  (t/is-status 201)
                  (t/location))
          abs-uri (str p/service-context (u/de-camelcase uri))
          job (-> session-user
                  (request abs-uri)
                  (t/body->edn)
                  (t/is-status 200)
                  (ltu/is-operation-present "stop")
                  (get-in [:response :body]))]

      (is (= "QUEUED" (:state job)))

      (-> session-user
          (request "/api/job")
          (t/body->edn)
          (t/is-status 200)
          (get-in [:response :body]))

      (-> session-admin
          (request abs-uri :request-method :delete)
          (t/body->edn)
          (t/is-status 200)))))
