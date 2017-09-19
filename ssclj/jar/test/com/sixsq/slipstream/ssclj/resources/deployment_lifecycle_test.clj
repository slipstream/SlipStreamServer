(ns com.sixsq.slipstream.ssclj.resources.deployment-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [ring.util.codec :as rc]
    [com.sixsq.slipstream.ssclj.resources.deployment :refer :all]
    [com.sixsq.slipstream.ssclj.resources.deployment-template :as dt]
    [com.sixsq.slipstream.ssclj.resources.deployment-parameter :as dp]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [com.sixsq.slipstream.ssclj.resources.zk.deployment.utils :as zdu]
    [com.sixsq.slipstream.ssclj.resources.deployment.utils :as du]
    [com.sixsq.slipstream.ssclj.resources.deployment.state-machine :as dsm]
    [com.sixsq.slipstream.ssclj.resources.deployment-template-std :as std]
    [com.sixsq.slipstream.ssclj.resources.deployment-std :as dstd]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [clj-http.fake :refer [with-fake-routes]]))

(use-fixtures :once t/setup-embedded-zk)

(defn fake-http-java-server [f]
  (with-fake-routes
    {"http://localhost:8182/run" (fn
                                   [request]
                                   {:status 201
                                    :headers
                                            {"Location"
                                             "http://localhost:8182/run/97089b96-5d99-4ccd-9bfe-99ba3ca21ae2"}})
     "http://localhost:8182/run/97089b96-5d99-4ccd-9bfe-99ba3ca21ae2"
                                 {:get  (fn [request] {:status 200
                                                       :body   (slurp "test-resources/deployment-service-testing.json")})
                                  :post (fn [request] {:status 200})}}
    (f)))

(use-fixtures :each (join-fixtures [t/with-test-es-client-fixture t/cleanup-all-zk-nodes fake-http-java-server]))

(def base-uri (str p/service-context resource-url))

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

;; initialize must to called to pull in SessionTemplate test examples
(dyn/initialize)

(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(deftest create-deployment

  (let [href (str dt/resource-url "/" std/method)
        template-url (str p/service-context dt/resource-url "/" std/method)
        session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "super ADMIN"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "test USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))
        template (-> session-admin
                     (request template-url)
                     (ltu/body->edn)
                     (ltu/is-status 200)
                     (get-in [:response :body]))

        no-href-create {:deploymentTemplate (strip-unwanted-attrs
                                              (assoc template
                                                :module "module/examples/tutorials/service-testing/system/1940"))}
        href-create {:deploymentTemplate {:href   href
                                          :module "module/examples/tutorials/service-testing/system/1940"}}
        invalid-create (assoc-in href-create [:deploymentTemplate :href] "deployment-template/unknown-template")]

    ;; create a deployment via user
    (let [create-req href-create
          resp (-> session-user
                   (request base-uri
                            :request-method :post
                            :body (json/write-str create-req))
                   (ltu/body->edn)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin should be able to see, edit, and delete deployment
      (-> session-user
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-operation-present "delete")
          (ltu/is-operation-absent "edit"))

      ;; admin can delete resource
      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))
    (let [create-req href-create
          resp (-> session-user
                   (request base-uri
                            :request-method :post
                            :body (json/write-str create-req))
                   (ltu/body->edn)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin should be able to see, and delete deployment
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-operation-present "delete")
          (ltu/is-operation-absent "edit"))

      (-> session-user
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-operation-present "delete")
          (ltu/is-operation-absent "edit"))

      ;; user can delete resource
      (-> session-user
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))))

(deftest start-deployment

  (let [href (str dt/resource-url "/" std/method)
        template-url (str p/service-context dt/resource-url "/" std/method)
        session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "super ADMIN"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "test USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))
        template (-> session-admin
                     (request template-url)
                     (ltu/body->edn)
                     (ltu/is-status 200)
                     (get-in [:response :body]))

        href-create {:deploymentTemplate {:href   href
                                          :module "module/examples/tutorials/service-testing/system/1940"}}]

    (let [create-req href-create
          resp (-> session-user
                   (request base-uri
                            :request-method :post
                            :body (json/write-str create-req))
                   (ltu/body->edn)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))
          deployment (-> session-user
                         (request abs-uri)
                         (ltu/body->edn))
          start-uri (str p/service-context
                         (t/get-op deployment "http://schemas.dmtf.org/cimi/2/action/start"))
          started-deployment (-> session-user
                                 (request start-uri)
                                 (t/body->edn)
                                 (t/is-status 200))]

      (is (not (get-in deployment [:body :start-time])))
      (is (get-in started-deployment [:response :body :start-time]))

      (is (= dsm/init-state (get-in started-deployment [:response :body :state])))

      (are [expected value]
        (= expected value)
        dsm/init-state (uzk/get-data (str zdu/separator uri "/state"))
        "Unknown" (uzk/get-data
                    (str zdu/separator uri "/" zdu/nodes-name "/apache/1/" "vmstate"))))))


(deftest update-deployment-state

  (let [href (str dt/resource-url "/" std/method)
        template-url (str p/service-context dt/resource-url "/" std/method)
        session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "super ADMIN"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "test USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))
        template (-> session-admin
                     (request template-url)
                     (ltu/body->edn)
                     (ltu/is-status 200)
                     (get-in [:response :body]))

        href-create {:deploymentTemplate {:href   href
                                          :module "module/examples/tutorials/service-testing/system/1940"}}]

    (let [create-req href-create
          resp (-> session-user
                   (request base-uri
                            :request-method :post
                            :body (json/write-str create-req))
                   (ltu/body->edn)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))
          deployment (-> session-user
                         (request abs-uri)
                         (ltu/body->edn))
          start-uri (str p/service-context
                         (t/get-op deployment "http://schemas.dmtf.org/cimi/2/action/start"))
          started-deployment (-> session-user
                                 (request start-uri)
                                 (t/body->edn)
                                 (t/is-status 200))
          abs-uri-deployment-parameter-state (str p/service-context dp/resource-url
                                                  "/" (du/deployment-href-to-uuid uri) "_state")
          update-deployment-parameter-state
          (-> session-user
              (request abs-uri-deployment-parameter-state :request-method :put
                       :body (json/write-str {:value dsm/provisioning-state}))
              (t/body->edn)
              (t/is-status 403))
          update-deployment-parameter-state
          (-> session-admin
              (request abs-uri-deployment-parameter-state :request-method :put
                       :body (json/write-str {:value dsm/provisioning-state}))
              (t/body->edn)
              (t/is-status 200))
          provisioning-deployment (-> session-user
                                      (request start-uri)
                                      (t/body->edn)
                                      (t/is-status 200))]

      (is (= dsm/provisioning-state (get-in provisioning-deployment [:response :body :state])))

      (is (= dsm/provisioning-state (uzk/get-data (str zdu/separator uri "/state"))))
      )))

(deftest update-deployment-move-states-abort

  (let [href (str dt/resource-url "/" std/method)
        template-url (str p/service-context dt/resource-url "/" std/method)
        session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "super ADMIN"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "test USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))
        template (-> session-admin
                     (request template-url)
                     (ltu/body->edn)
                     (ltu/is-status 200)
                     (get-in [:response :body]))

        href-create {:deploymentTemplate {:href   href
                                          :module "module/examples/tutorials/service-testing/system/1940"}}]

    (let [create-req href-create
          resp (-> session-user
                   (request base-uri
                            :request-method :post
                            :body (json/write-str create-req))
                   (ltu/body->edn)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))
          deployment (-> session-user
                         (request abs-uri)
                         (ltu/body->edn))
          start-uri (str p/service-context
                         (t/get-op deployment "http://schemas.dmtf.org/cimi/2/action/start"))
          started-deployment (-> session-user
                                 (request start-uri)
                                 (t/body->edn)
                                 (t/is-status 200))
          abs-uri-deployment-parameter-state (str p/service-context dp/resource-url
                                                  "/" (du/deployment-href-to-uuid uri) "_state")
          update-deployment-parameter-state
          (-> session-user
              (request abs-uri-deployment-parameter-state :request-method :put
                       :body (json/write-str {:value dsm/provisioning-state}))
              (t/body->edn)
              (t/is-status 403))
          update-deployment-parameter-state
          (-> session-admin
              (request abs-uri-deployment-parameter-state :request-method :put
                       :body (json/write-str {:value dsm/provisioning-state}))
              (t/body->edn)
              (t/is-status 200))
          provisioning-deployment (-> session-user
                                      (request start-uri)
                                      (t/body->edn)
                                      (t/is-status 200))]

      (-> session-admin
          (request (str p/service-context (du/deployment-parameter-href
                                            {:deployment {:href uri} :node-name "testclient"
                                             :node-index 1 :name "complete"}))
                   :request-method :put :body (json/write-str {:value dsm/provisioning-state}))
          (t/body->edn)
          (t/is-status 200))
      (-> session-admin
          (request (str p/service-context (du/deployment-parameter-href
                                            {:deployment {:href uri} :node-name "apache"
                                             :node-index 1 :name "complete"}))
                   :request-method :put :body (json/write-str {:value dsm/provisioning-state}))
          (t/body->edn)
          (t/is-status 200))

      (-> session-admin
          (request (str p/service-context (du/deployment-parameter-href
                                            {:deployment {:href uri} :node-name "apache"
                                             :node-index 1 :name "abort"}))
                   :request-method :put :body (json/write-str {:value "Error in node 1 abort the run"}))
          (t/body->edn)
          (t/is-status 200))

      (is (= 1 (count (uzk/children
                        (zdu/deployment-state-path uri)))))

      (is (= dsm/aborted-state (uzk/get-data (str zdu/separator uri "/state"))))

      (is (= dsm/aborted-state (-> session-user
                                   (request (str p/service-context uri))
                                   (t/body->edn)
                                   (get-in [:response :body :state]))))
      )))
