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
                                 (fn [request] {:status 200
                                                :body   (slurp "test-resources/deployment-service-testing.json")})
     }
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

(def valid-entry-deployment-uuid "dfd34916-6ede-47f7-aaeb-a30ddecbba5c")

(def valid-entry
  {:id                  (str resource-url "/" valid-entry-deployment-uuid)
   :resourceURI         resource-uri
   :module-resource-uri "module/examples/tutorials/service-testing/system/1940"
   :category            "Deployment"
   :type                "Orchestration"
   :mutable             false
   :keep-running        true
   :nodes               {:node1 {:parameters         {:cloudservice {:description "p1 description" ;TODO only node name and  multiplicity are being used for now from all these parameters
                                                                     :value       "abc"}
                                                      :multiplicity {:value "2"}}
                                 :runtime-parameters {:p1 {:description "p1 description"
                                                           :value       "abc"
                                                           :mapped-to   "a"}}}
                         :node2 {:parameters {:cloudservice {:description "param1 description"
                                                             :value       "abc"}}}}})


(deftest lifecycle
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
          (ltu/is-status 200)))

    ))

;
;(deftest create-deployment
;
;  (let [session-admin-json (-> (session (ring-app))
;                               (content-type "application/json")
;                               (header authn-info-header "super ADMIN USER ANON"))
;        session-admin-form (-> (session (ring-app))
;                               (content-type "application/x-www-form-urlencoded")
;                               (header authn-info-header "super ADMIN USER ANON"))
;        session-user (-> (session (ring-app))
;                         (content-type "application/json")
;                         (header authn-info-header "jane USER ANON"))
;        session-anon (-> (session (ring-app))
;                         (content-type "application/json"))]
;
;    ;; adding, retrieving and  deleting entry as user should succeed
;    (let [deployment-href (-> session-user
;                              (request base-uri
;                                       :request-method :post
;                                       :body (json/write-str valid-entry))
;                              (t/body->edn)
;                              (t/is-status 201)
;                              (t/location))
;          abs-uri (str p/service-context (u/de-camelcase deployment-href))
;          created-deployment (-> session-user
;                                 (request abs-uri)
;                                 (t/body->edn)
;                                 (t/is-status 200))]
;
;      (is (not (uzk/exists (str zdu/separator deployment-href)))))))
;
;
;(deftest start-deployment
;
;  (let [session-admin-json (-> (session (ring-app))
;                               (content-type "application/json")
;                               (header authn-info-header "super ADMIN USER ANON"))
;        session-admin-form (-> (session (ring-app))
;                               (content-type "application/x-www-form-urlencoded")
;                               (header authn-info-header "super ADMIN USER ANON"))
;        session-user (-> (session (ring-app))
;                         (content-type "application/json")
;                         (header authn-info-header "jane USER ANON"))
;        session-anon (-> (session (ring-app))
;                         (content-type "application/json"))]
;
;    ;; adding, retrieving and  deleting entry as user should succeed
;    (let [deployment-href (-> session-user
;                              (request base-uri
;                                       :request-method :post
;                                       :body (json/write-str valid-entry))
;                              (t/body->edn)
;                              (t/is-status 201)
;                              (t/location))
;          abs-uri (str p/service-context (u/de-camelcase deployment-href))
;          created-deployment (-> session-user
;                                 (request abs-uri)
;                                 (t/body->edn)
;                                 (t/is-status 200))
;          start-uri (str p/service-context
;                         (t/get-op created-deployment "http://schemas.dmtf.org/cimi/2/action/start"))
;          started-deployment (-> session-user
;                                 (request start-uri)
;                                 (t/body->edn)
;                                 (t/is-status 200))
;          ]
;
;      (is (not (get-in created-deployment [:body :start-time])))
;      (is (get-in started-deployment [:response :body :start-time]))
;
;      (is (= dsm/init-state (get-in started-deployment [:response :body :state])))
;
;      (are [expected value]
;        (= expected value)
;        dsm/init-state (uzk/get-data (str zdu/separator deployment-href "/state"))
;        "unknown" (uzk/get-data
;                    (str zdu/separator deployment-href "/" zdu/nodes-name "/node2/1/" "vmstate"))))))
;
;(deftest create-deployment-update-deployment-state
;
;  (let [session-admin-json (-> (session (ring-app))
;                               (content-type "application/json")
;                               (header authn-info-header "super ADMIN USER ANON"))
;        session-user (-> (session (ring-app))
;                         (content-type "application/json")
;                         (header authn-info-header "jane USER ANON"))]
;
;    ;; adding, retrieving and  deleting entry as user should succeed
;    (let [deployment-href (-> session-user
;                              (request base-uri
;                                       :request-method :post
;                                       :body (json/write-str valid-entry))
;                              (t/body->edn)
;                              (t/is-status 201)
;                              (t/location))
;          abs-uri (str p/service-context (u/de-camelcase deployment-href))
;          abs-uri-deployment-parameter-state (str p/service-context dp/resource-url
;                                                  "/" valid-entry-deployment-uuid "_state")
;          created-deployment (-> session-user
;                                 (request abs-uri)
;                                 (t/body->edn)
;                                 (t/is-status 200))
;          start-uri (str p/service-context
;                         (t/get-op created-deployment "http://schemas.dmtf.org/cimi/2/action/start"))
;          started-deployment (-> session-user
;                                 (request start-uri)
;                                 (t/body->edn)
;                                 (t/is-status 200))
;          update-deployment-parameter-state
;          (-> session-user
;              (request abs-uri-deployment-parameter-state :request-method :put
;                       :body (json/write-str {:value dsm/provisioning-state}))
;              (t/body->edn)
;              (t/is-status 403))
;          update-deployment-parameter-state
;          (-> session-admin-json
;              (request abs-uri-deployment-parameter-state :request-method :put
;                       :body (json/write-str {:value dsm/provisioning-state}))
;              (t/body->edn)
;              (t/is-status 200))
;          provisioning-deployment (-> session-user
;                                      (request start-uri)
;                                      (t/body->edn)
;                                      (t/is-status 200))]
;
;      (is (= dsm/provisioning-state (get-in provisioning-deployment [:response :body :state])))
;
;      (is (= dsm/provisioning-state (uzk/get-data (str zdu/separator deployment-href "/state"))))
;      )))
;
;(deftest create-deployment-move-states
;
;  (let [session-admin-json (-> (session (ring-app))
;                               (content-type "application/json")
;                               (header authn-info-header "super ADMIN USER ANON"))
;        session-user (-> (session (ring-app))
;                         (content-type "application/json")
;                         (header authn-info-header "jane USER ANON"))]
;
;    ;; adding, retrieving and  deleting entry as user should succeed
;    (let [deployment-href (-> session-user
;                              (request base-uri
;                                       :request-method :post
;                                       :body (json/write-str valid-entry))
;                              (t/body->edn)
;                              (t/location))
;          abs-uri (str p/service-context (u/de-camelcase deployment-href))
;          abs-uri-deployment-parameter-state (str p/service-context dp/resource-url
;                                                  "/" valid-entry-deployment-uuid "_state")
;          created-deployment (-> session-user
;                                 (request abs-uri)
;                                 (t/body->edn))
;          start-uri (str p/service-context
;                         (t/get-op created-deployment "http://schemas.dmtf.org/cimi/2/action/start"))
;          started-deployment (-> session-user
;                                 (request start-uri)
;                                 (t/body->edn))
;          update-deployment-parameter-state
;          (-> session-admin-json
;              (request abs-uri-deployment-parameter-state :request-method :put
;                       :body (json/write-str {:value dsm/provisioning-state}))
;              (t/body->edn)
;              (t/is-status 200))
;
;          provisioning-deployment (-> session-user
;                                      (request start-uri)
;                                      (t/body->edn)
;                                      (t/is-status 200))]
;      (is (= dsm/provisioning-state (get-in provisioning-deployment [:response :body :state])))
;
;      (is (= dsm/provisioning-state (uzk/get-data (str zdu/separator deployment-href "/state"))))
;
;      (-> session-admin-json
;          (request (str p/service-context (du/deployment-parameter-href
;                                            {:deployment {:href deployment-href} :node-name "node1"
;                                             :node-index 1 :name "state-complete"}))
;                   :request-method :put :body (json/write-str {:value dsm/provisioning-state}))
;          (t/body->edn)
;          (t/is-status 200))
;      (-> session-admin-json
;          (request (str p/service-context (du/deployment-parameter-href
;                                            {:deployment {:href deployment-href} :node-name "node1"
;                                             :node-index 2 :name "state-complete"}))
;                   :request-method :put :body (json/write-str {:value dsm/provisioning-state}))
;          (t/body->edn)
;          (t/is-status 200))
;
;      (is (= 1 (count (uzk/children
;                        (zdu/deployment-state-path deployment-href)))))
;
;      (-> session-admin-json
;          (request (str p/service-context (du/deployment-parameter-href
;                                            {:deployment {:href deployment-href} :node-name "node2"
;                                             :node-index 1 :name "state-complete"}))
;                   :request-method :put :body (json/write-str {:value dsm/provisioning-state}))
;          (t/body->edn)
;          (t/is-status 200))
;
;      (is (= dsm/executing-state (uzk/get-data (str zdu/separator deployment-href "/state"))))
;
;      (is (= dsm/executing-state (-> session-user
;                                     (request (str p/service-context deployment-href))
;                                     (t/body->edn)
;                                     (get-in [:response :body :state]))))
;
;      (is (= 3 (count (uzk/children
;                        (zdu/deployment-state-path deployment-href)))))
;
;      (-> session-admin-json
;          (request (str p/service-context (du/deployment-parameter-href
;                                            {:deployment {:href deployment-href} :node-name "node1"
;                                             :node-index 1 :name "state-complete"}))
;                   :request-method :put :body (json/write-str {:value dsm/executing-state}))
;          (t/body->edn)
;          (t/is-status 200))
;      (-> session-admin-json
;          (request (str p/service-context (du/deployment-parameter-href
;                                            {:deployment {:href deployment-href} :node-name "node1"
;                                             :node-index 2 :name "state-complete"}))
;                   :request-method :put :body (json/write-str {:value dsm/executing-state}))
;          (t/body->edn)
;          (t/is-status 200))
;
;      (-> session-admin-json
;          (request (str p/service-context (du/deployment-parameter-href
;                                            {:deployment {:href deployment-href} :node-name "node2"
;                                             :node-index 1 :name "state-complete"}))
;                   :request-method :put :body (json/write-str {:value dsm/executing-state}))
;          (t/body->edn)
;          (t/is-status 200))
;
;      (is (= dsm/sending-report-state (-> session-user
;                                          (request (str p/service-context deployment-href))
;                                          (t/body->edn)
;                                          (get-in [:response :body :state]))))
;
;      )))
;
;
;(deftest create-deployment-move-states-abort
;
;  (let [session-admin-json (-> (session (ring-app))
;                               (content-type "application/json")
;                               (header authn-info-header "super ADMIN USER ANON"))
;        session-user (-> (session (ring-app))
;                         (content-type "application/json")
;                         (header authn-info-header "jane USER ANON"))]
;
;    ;; adding, retrieving and  deleting entry as user should succeed
;    (let [deployment-href (-> session-user
;                              (request base-uri
;                                       :request-method :post
;                                       :body (json/write-str valid-entry))
;                              (t/body->edn)
;                              (t/location))
;          abs-uri (str p/service-context (u/de-camelcase deployment-href))
;          abs-uri-deployment-parameter-state (str p/service-context dp/resource-url
;                                                  "/" valid-entry-deployment-uuid "_state")
;          created-deployment (-> session-user
;                                 (request abs-uri)
;                                 (t/body->edn))
;          start-uri (str p/service-context
;                         (t/get-op created-deployment "http://schemas.dmtf.org/cimi/2/action/start"))
;          started-deployment (-> session-user
;                                 (request start-uri)
;                                 (t/body->edn))
;          update-deployment-parameter-state
;          (-> session-admin-json
;              (request abs-uri-deployment-parameter-state :request-method :put
;                       :body (json/write-str {:value dsm/provisioning-state}))
;              (t/body->edn)
;              (t/is-status 200))]
;
;      (-> session-admin-json
;          (request (str p/service-context (du/deployment-parameter-href
;                                            {:deployment {:href deployment-href} :node-name "node1"
;                                             :node-index 1 :name "state-complete"}))
;                   :request-method :put :body (json/write-str {:value dsm/provisioning-state}))
;          (t/body->edn)
;          (t/is-status 200))
;      (-> session-admin-json
;          (request (str p/service-context (du/deployment-parameter-href
;                                            {:deployment {:href deployment-href} :node-name "node1"
;                                             :node-index 2 :name "state-complete"}))
;                   :request-method :put :body (json/write-str {:value dsm/provisioning-state}))
;          (t/body->edn)
;          (t/is-status 200))
;
;      (-> session-admin-json
;          (request (str p/service-context (du/deployment-parameter-href
;                                            {:deployment {:href deployment-href} :node-name "node2"
;                                             :node-index 1 :name "abort"}))
;                   :request-method :put :body (json/write-str {:value "Error in node 1 abort the run"}))
;          (t/body->edn)
;          (t/is-status 200))
;
;      (is (= 1 (count (uzk/children
;                        (zdu/deployment-state-path deployment-href)))))
;
;      (is (= dsm/aborted-state (uzk/get-data (str zdu/separator deployment-href "/state"))))
;
;      (is (= dsm/aborted-state (-> session-user
;                                   (request (str p/service-context deployment-href))
;                                   (t/body->edn)
;                                   (get-in [:response :body :state]))))
;
;      )))